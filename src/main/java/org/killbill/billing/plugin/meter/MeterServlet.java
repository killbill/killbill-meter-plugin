/*
 * Copyright 2015 Trifon Trifonov
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.meter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.killbill.billing.plugin.core.PluginServlet;
import org.killbill.billing.plugin.meter.api.TimeAggregationMode;
import org.killbill.billing.plugin.meter.api.user.MeterUserApi;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.CallOrigin;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.callcontext.UserType;
import org.killbill.clock.Clock;
import org.osgi.service.log.LogService;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


public class MeterServlet extends PluginServlet {

	private static final long serialVersionUID = 1L;

	private static final String QUERY_METER_WITH_CATEGORY_AGGREGATE = "withCategoryAggregate";
	private static final String QUERY_METER_TIME_AGGREGATION_MODE = "timeAggregationMode";
	private static final String QUERY_METER_TIMESTAMP = "timestamp";
	private static final String QUERY_METER_FROM = "from";
	private static final String QUERY_METER_TO = "to";
	private static final String QUERY_METER_CATEGORY = "category";
	private static final String QUERY_METER_CATEGORY_AND_METRIC = "category_and_metric";

	private final LogService logService;

	private final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

	private final MeterUserApi meterUserApi;
	private final Clock clock;


	public MeterServlet(final LogService logService, final MeterUserApi meterUserApi, final Clock clock) {
		super();
		this.logService = logService;
		this.meterUserApi = meterUserApi;
		this.clock = clock;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException 
	{
		// Find me on http://localhost:8080/plugins/killbill-meter
		logService.log(LogService.LOG_INFO, "Request to Meter plugin Servlet.");

		String[] pathData = extractPathData( req.getPathInfo() );
		if (pathData != null && pathData.length == 1) {
			final String source = pathData[0];
			// Aggregates per category
			final List<String> categories = extractList( req.getParameter( QUERY_METER_CATEGORY ) );
			
			// Format: category,metric
			final List<String> categoriesAndMetrics = extractList( req.getParameter( QUERY_METER_CATEGORY_AND_METRIC ) );
			final String fromTimestampString = req.getParameter( QUERY_METER_FROM );
			final String toTimestampString = req.getParameter( QUERY_METER_TO );
			String timeAggregationModeString = req.getParameter( QUERY_METER_TIME_AGGREGATION_MODE );
			if (timeAggregationModeString == null) {
				timeAggregationModeString = "";
			}
			final TenantContext tenantContext = createTenantContext( req );
			
			final DateTime fromTimestamp;
			if (fromTimestampString != null) {
				fromTimestamp = DATE_TIME_FORMATTER.parseDateTime(fromTimestampString);
			} else {
				fromTimestamp = clock.getUTCNow().minusMonths(3);
			}
			final DateTime toTimestamp;
			if (toTimestampString != null) {
				toTimestamp = DATE_TIME_FORMATTER.parseDateTime(toTimestampString);
			} else {
				toTimestamp = clock.getUTCNow();
			}
	
			ServletOutputStream output = resp.getOutputStream();
			try {
				// Look at aggregates per category?
				if (categories != null && categories.size() > 0) {
					if (Strings.isNullOrEmpty(timeAggregationModeString)) {
						meterUserApi.getUsage(output, source, categories, fromTimestamp, toTimestamp, tenantContext);
					} else {
						final TimeAggregationMode timeAggregationMode = TimeAggregationMode.valueOf(timeAggregationModeString);
						meterUserApi.getUsage(output, timeAggregationMode, source, categories, fromTimestamp, toTimestamp, tenantContext);
					}
				} else {
					final Map<String, Collection<String>> metricsPerCategory = retrieveMetricsPerCategory(categoriesAndMetrics);
					if (Strings.isNullOrEmpty(timeAggregationModeString)) {
						meterUserApi.getUsage(output, source, metricsPerCategory, fromTimestamp, toTimestamp, tenantContext);
					} else {
						final TimeAggregationMode timeAggregationMode = TimeAggregationMode.valueOf(timeAggregationModeString);
						meterUserApi.getUsage(output, timeAggregationMode, source, metricsPerCategory, fromTimestamp, toTimestamp, tenantContext);
					}
				}
			} finally {
				output.close();
			}
		}
	}

	// REQUEST: curl -vk -X POST "plugins/killbill-meter/sourceName/categoryName/metricName?withCategoryAggregate=true"
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		logService.log(LogService.LOG_INFO, "reg.contextPath = " + req.getContextPath() );
		logService.log(LogService.LOG_INFO, "reg.pathInfo    = " + req.getPathInfo() );
		logService.log(LogService.LOG_INFO, "reg.servletPath = " + req.getServletPath() );
		
		String[] pathData = extractPathData( req.getPathInfo() );
		if (pathData != null && pathData.length == 3) {
			final String source = pathData[0];
			final String categoryName = pathData[1];
			final String metricName = pathData[2];

			Boolean withAggregate = Boolean.FALSE;
//			Map<String, String[]> paramMap = req.getParameterMap();
//			if (paramMap.get( QUERY_METER_WITH_CATEGORY_AGGREGATE ) != null) {
//				withAggregate = Boolean.valueOf( paramMap.get( QUERY_METER_WITH_CATEGORY_AGGREGATE )[0] );
//			}
			String withAggregateStr = req.getParameter( QUERY_METER_WITH_CATEGORY_AGGREGATE );
			if (withAggregateStr != null) {
				withAggregate = Boolean.valueOf( withAggregateStr );
			}

			final String timestampString = req.getParameter( QUERY_METER_TIMESTAMP );
			
			final String createdBy = getCreatedBy( req );
			final String reason = getReason( req );
			final String comment = getComment( req );
			
			final CallContext callContext = createCallContext(createdBy, reason, comment, req);

			final DateTime timestamp;
			if (timestampString == null) {
				timestamp = clock.getUTCNow();
			} else {
				timestamp = DATE_TIME_FORMATTER.parseDateTime(timestampString);
			}

			if (withAggregate) {
				meterUserApi.incrementUsageAndAggregate(source, categoryName, metricName, timestamp, callContext);
			} else {
				meterUserApi.incrementUsage(source, categoryName, metricName, timestamp, callContext);
			}
		}
	}
/*
		final String formUrlEncoded = getRequestData(req);
		final String[] keyValuePairs = formUrlEncoded.split("\\&");
		final Map<String, String> parameters = new HashMap<String, String>();
		for (final String keyValuePair : keyValuePairs) {
			if (keyValuePair != null && !keyValuePair.isEmpty()) {
				final String[] keyValue = keyValuePair.split("=");
				if (keyValue.length != 2) {
					throw new RuntimeException("Invalid parameters :" + formUrlEncoded);
				}
				parameters.put(keyValue[0], keyValue[1]);
			}
		}

		final StringBuffer tmp = new StringBuffer("TermUrl parameters:\n\n");
		for (final String key : parameters.keySet()) {
			tmp.append(key).append(": ").append(parameters.get(key)).append("\n\n");
		}

		logger.info(tmp.toString());
	}
*/

	private String[] extractPathData(String pathInfo) {
		String[] result = null;
		
		String tmp = pathInfo == null ? "" : pathInfo;
		if (tmp.startsWith("/")) {
			tmp = tmp.substring( 1 ); // remove begging "/"
		}
		result = tmp.split("/");
		return result;
	}
	private List<String> extractList(String str) {
		String[] result = null;
		
		String tmp = str == null ? "" : str;
		if (tmp.startsWith(",")) {
			tmp = tmp.substring( 1 ); // remove begging ","
		}
		result = tmp.split(",");
		return Arrays.asList( result );
	}

	private Map<String, Collection<String>> retrieveMetricsPerCategory(final List<String> categoriesAndMetrics) {
		final Map<String, Collection<String>> metricsPerCategory = new HashMap<String, Collection<String>>();
		for (final String categoryAndSampleKind : categoriesAndMetrics) {
			final String[] categoryAndMetric = getCategoryAndMetricFromQueryParameter(categoryAndSampleKind);
			if (metricsPerCategory.get(categoryAndMetric[0]) == null) {
				metricsPerCategory.put(categoryAndMetric[0], new ArrayList<String>());
			}
			metricsPerCategory.get(categoryAndMetric[0]).add(categoryAndMetric[1]);
		}
		return metricsPerCategory;
	}

	private String[] getCategoryAndMetricFromQueryParameter(final String categoryAndMetric) {
		final String[] parts = categoryAndMetric.split(",");
		if (parts.length != 2) {
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
		return parts;
	}

	private CallContext createCallContext(final String createdBy, final String reason, final String comment, final ServletRequest request)
			throws IllegalArgumentException
	{
		try {
			Preconditions.checkNotNull(createdBy, String.format("Header %s needs to be set", HDR_CREATED_BY));
			final Tenant tenant = getTenantFromRequest(request);
			return new MeterCallContext(tenant == null ? null : tenant.getId(),
					createdBy, CallOrigin.EXTERNAL, UserType.CUSTOMER, reason,
					comment, UUID.randomUUID(), clock.getUTCNow());
		} catch (NullPointerException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	private TenantContext createTenantContext(final ServletRequest request) {
		final Tenant tenant = getTenantFromRequest(request);
		if (tenant == null) {
			// Multi-tenancy may not have been configured - default to "default"
			// tenant (see InternalCallContextFactory)
			return new MeterTenantContext();
		} else {
			return new MeterTenantContext(tenant.getId());
		}
	}

	private Tenant getTenantFromRequest(final ServletRequest request) {
		final Object tenantObject = request.getAttribute("killbill_tenant");
		if (tenantObject == null) {
			return null;
		} else {
			return (Tenant) tenantObject;
		}
	}
}