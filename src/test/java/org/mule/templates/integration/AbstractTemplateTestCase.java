/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

/**
 * This is the base test class for Templates integration tests.
 */
public abstract class AbstractTemplateTestCase extends FunctionalTestCase {

	private static final String MAPPINGS_FOLDER_PATH = "./mappings";
	private static final String TEST_FLOWS_FOLDER_PATH = "./src/test/resources/flows/";
	private static final String MULE_DEPLOY_PROPERTIES_PATH = "./src/main/app/mule-deploy.properties";

	protected static final int TIMEOUT_SEC = 300;
	protected static final String TEMPLATE_NAME = "opportunity-broadcast";

	protected Flow retrieveSalesOrderFromSapFlow;
	protected Flow retrieveAccountFromSapFlow;

	@Rule
	public DynamicPort port = new DynamicPort("http.port");

	@Override
	protected String getConfigResources() {
		String resources = "";
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(MULE_DEPLOY_PROPERTIES_PATH));
			resources = props.getProperty("config.resources");
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not find mule-deploy.properties file on classpath. Please add any of those files or override the getConfigResources() method to provide the resources by your own.");
		}

		return resources + getTestFlows();
	}

	protected String getTestFlows() {
		File[] listOfFiles = new File(TEST_FLOWS_FOLDER_PATH).listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isFile() && f.getName().endsWith("xml");
			}
		});
		
		if (listOfFiles == null) {
			return "";
		}
		
		StringBuilder resources = new StringBuilder();
		for (File f : listOfFiles) {
			resources.append(",").append(TEST_FLOWS_FOLDER_PATH).append(f.getName());
		}
		return resources.toString();
	}

	@Override
	protected Properties getStartUpProperties() {
		Properties properties = new Properties(super.getStartUpProperties());

		String pathToResource = MAPPINGS_FOLDER_PATH;
		File graphFile = new File(pathToResource);

		properties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, graphFile.getAbsolutePath());

		return properties;
	}

	protected void deleteTestOpportunityFromSandBox(List<Map<String, Object>> createdOpportunities) throws Exception {
		List<String> idList = new ArrayList<String>();

		// Delete the created opportunities in Salesforce
		Flow flow = lookupFlowConstruct("deleteOpportunityFromSalesforceFlow");
//		flow.initialise();
		for (Map<String, Object> c : createdOpportunities) {
			idList.add((String) c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();

		// Delete the created opportunities in SAP
		flow = lookupFlowConstruct("deleteSalesOrderFromSapFlow");
//		flow.initialise();
		for (Map<String, Object> c : createdOpportunities) {
			System.out.println("CCC: " + c);
			Map<String, Object> opportunity = invokeRetrieveFlow(retrieveSalesOrderFromSapFlow, c);
			if (opportunity != null) {
				idList.add((String) opportunity.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	protected void deleteTestAccountFromSandBox(List<Map<String, Object>> createdAccounts) throws Exception {
		// Delete the created accounts in Salesforce
		Flow flow = lookupFlowConstruct("deleteAccountFromSalesforceFlow");
//		flow.initialise();

		List<Object> idList = new ArrayList<Object>();
		for (Map<String, Object> c : createdAccounts) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created accounts in SAP
		flow = lookupFlowConstruct("deleteAccountFromSapFlow");
//		flow.initialise();
		idList.clear();
		for (Map<String, Object> c : createdAccounts) {
			Map<String, Object> account = invokeRetrieveFlow(retrieveAccountFromSapFlow, c);
			if (account != null) {
				System.out.println("AAAAACCCCC: " + account);
				idList.add(account.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	protected String buildUniqueName(String templateName, String name) {
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(name);
		builder.append(templateName);
		builder.append(timeStamp);

		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> invokeRetrieveFlow(Flow flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));

		Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}

	protected Map<String, Object> createOpportunity(String orgId, int sequence) throws ParseException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Name", buildUniqueName(TEMPLATE_NAME, "OppName" + sequence + "_"));
		map.put("StageName", "NoStage");
		map.put("CloseDate", date("2050-10-10"));
		map.put("Probability", "1");
		return map;
	}

	private Date date(String dateString) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
	}

	protected String buildUniqueEmail(String user) {
		String server = "fakemail";

		StringBuilder builder = new StringBuilder();
		builder.append(buildUniqueName(TEMPLATE_NAME, user));
		builder.append("@");
		builder.append(server);
		builder.append(".com");

		return builder.toString();
	}
}
