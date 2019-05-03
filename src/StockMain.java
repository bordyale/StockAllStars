import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import au.com.bytecode.opencsv.CSVWriter;

public class StockMain {

	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) throws ParseException {
		// TODO Auto-generated method stub
		StockMain http = new StockMain();

		// Regression
		// double[] x = { 1, 2, 3, 4, 5 };
		// double[] y = { 2, 4.0, 4.4, 7.1, 4 };
		// LinearRegression regr = new LinearRegression(x, y);
		// System.out.println("Slope: " + regr.slope());
		// System.out.println("R2: " + regr.R2());
		// System.out.println("intercept: " + regr.intercept());

		String[] symbols = { "AI.PA", "ALV.DE", "ABI.BR", "ADS.DE", "ENI.MI" };

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date datum = format.parse("2019-05-03");

		Map<String, Map<String, Object>> stocksMap = new HashMap<String, Map<String, Object>>();
		for (int i = 0; i < symbols.length; i++) {
			Map<String, Object> stockMap = new HashMap<String, Object>();
			System.out.println("Testing " + i + " - Send Http GET request");
			try {
				JSONObject resp = http.sendGet(symbols[i]);
				JSONObject arr = resp.getJSONObject("Time Series (Daily)");
				SortedMap<String, String> stockdata = new TreeMap<String, String>(Collections.reverseOrder());
				Iterator it = arr.keys();
				while (it.hasNext()) {
					String key = (String) it.next();
					stockdata.put(key, arr.getJSONObject(key).getString("4. close"));
				}
				System.out.println(stockdata.toString());
				stockMap.put("data", stockdata);

				// List<String> list = new
				// ArrayList<String>(stockdata.keySet());

				// Collections.reverse(list);

				double[] x = new double[90];
				double[] y = new double[90];
				int j = 0;
				Set<String> set = stockdata.keySet();
				for (String value : set) {
					Date valueDate = format.parse(value);

					if (valueDate.before(datum)) {

						if (j < 90) {
							x[j] = j;
							y[89 - j] = new Double(stockdata.get(value));
							System.out.println(value + " - - -" + stockdata.get(value));
							j++;
						} else {
							break;
						}
					}

				}
				for (int k = 0; k < x.length; k++) {
					System.out.println("Reg arrays: " + x[k] + " " + y[k]);
				}
				LinearRegression regr = new LinearRegression(x, y);
				System.out.println("Slope: " + regr.slope());
				System.out.println("R2: " + regr.R2());
				System.out.println("intercept: " + regr.intercept());
				stockMap.put("slope", regr.slope());
				stocksMap.put(symbols[i], stockMap);

				exportDataToExcel("E:/" + symbols[i] + ".csv", y);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// elaborate Slope table
		elaborateSlopeTable(stocksMap);

	}

	private JSONObject sendGet(String symbol) throws Exception {

		String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + symbol + "&apikey=IJMOD1FFWEBG5VWY";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		// int responseCode = con.getResponseCode();
		// System.out.println("\nSending 'GET' request to URL : " + url);
		// System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		JSONObject jsonObj = new JSONObject(response.toString());

		// print result
		System.out.println(jsonObj.toString());
		System.out.println(jsonObj.getJSONObject("Meta Data").get("2. Symbol"));
		return jsonObj;

	}

	public static void exportDataToExcel(String fileName, double[] data) throws FileNotFoundException, IOException {
		File file = new File(fileName);
		if (!file.isFile())
			file.createNewFile();

		CSVWriter csvWriter = new CSVWriter(new FileWriter(file));

		// for (int i = 0; i < rowCount; i++) {
		// int columnCount = data[i].length;
		String[] values = new String[data.length];
		for (int j = 0; j < data.length; j++) {
			values[j] = Double.toString(data[j]).replace(".", ",");
		}
		csvWriter.writeNext(values);

		csvWriter.flush();
		csvWriter.close();
	}

	private static void elaborateSlopeTable(Map<String, Map<String, Object>> stocksMap) {
		System.out.println("Slope table not ordered:");
		for (String key : stocksMap.keySet()) {
			System.out.println(key + " --- " + stocksMap.get(key).get("slope"));
		}

		System.out.println("Slope table ordered:");
		Set<Entry<String, Map<String, Object>>> set = stocksMap.entrySet();
		List<Entry<String, Map<String, Object>>> list = new ArrayList<Entry<String, Map<String, Object>>>(set);
		Collections.sort(list, new Comparator<Map.Entry<String, Map<String, Object>>>() {
			public int compare(Map.Entry<String, Map<String, Object>> o1, Map.Entry<String, Map<String, Object>> o2) {
				return ((Double) o1.getValue().get("slope")).compareTo((Double) o2.getValue().get("slope"));// Ascending
				// order
				// return (o2.getValue()).compareTo( o1.getValue()
				// );//Descending order
			}
		});
		for (Map.Entry<String, Map<String, Object>> entry : list) {
			System.out.println(entry.getKey() + " ==== " + entry.getValue().get("slope"));
		}

	}

}
