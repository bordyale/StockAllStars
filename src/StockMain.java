import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

//import au.com.bytecode.opencsv.CSVWriter;

public class StockMain {

	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) throws ParseException {
		// TODO Auto-generated method stub

		// Regression
		// double[] x = { 1, 2, 3, 4, 5 };
		// double[] y = { 2, 4.0, 4.4, 7.1, 4 };
		// LinearRegression regr = new LinearRegression(x, y);
		// System.out.println("Slope: " + regr.slope());
		// System.out.println("R2: " + regr.R2());
		// System.out.println("intercept: " + regr.intercept());

		String[] symbols = { "EMN", "KO", "HNI", "O", "LYB", "AIG", "WFC", "BAC", "AFL", "JPM", "TROW", "MAIN", "TD", "RY", "LNC" };
		// 1 = monthly
		// 3 = quaterly
		Integer[] divRate = { 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
		String[] years = { "2020", "2019", "2018", "2017", "2016", "2015", "2014" };

		String[] apikey = { "IJMOD1FFWEBG5VWY", "IJMOD1FFWEBG5VWY", "IJMOD1FFWEBG5VWY", "IJMOD1FFWEBG5VWY", "IJMOD1FFWEBG5VWY", "TCWC4KTESJIY8UL5", "TCWC4KTESJIY8UL5", "TCWC4KTESJIY8UL5",
				"TCWC4KTESJIY8UL5", "TCWC4KTESJIY8UL5", "WRFKAPP9TCNWQUKC", "WRFKAPP9TCNWQUKC", "WRFKAPP9TCNWQUKC", "WRFKAPP9TCNWQUKC", "WRFKAPP9TCNWQUKC", "NRXCKC71QSMJQZYA", "NRXCKC71QSMJQZYA",
				"NRXCKC71QSMJQZYA", "NRXCKC71QSMJQZYA", "NRXCKC71QSMJQZYA" };

		String[] apikey2 = { "IJMOD1FFWEBG5VWY" };

		// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		// Date datum = format.parse("2019-05-06");
		// Date datum = new Date();

		StockMain inst = new StockMain();
		Map<String, Map<String, Object>> stocksMap = inst.getStocksData(symbols, divRate, years, apikey2);

		System.out.println("StocksMap : " + stocksMap.toString());

	}

	private Map<String, Map<String, Object>> getStocksData(String[] symbols, Integer[] divRate, String[] years, String[] apikey) {

		String currentYear = new Integer(Calendar.getInstance().get(Calendar.YEAR)).toString();

		Map<String, Map<String, Object>> stocksMap = new HashMap<String, Map<String, Object>>();
		int apiindex = 0;
		// int apiindex2 = 1;
		boolean reset = false;
		for (int i = 0; i < symbols.length; i++) {
			Map<String, Object> stockMap = new HashMap<String, Object>();

			// System.out.println("Testing " + i + " - Send Http GET request");
			try {
				// String apikey = getApiKey();

				// int apiindexres = apiindex * apiindex2;
				if (i != 0 && i % (5 * apikey.length) == 0) {
					Thread.sleep(65000);
					apiindex = 0;
					reset = true;
				}
				if (i != 0 && i % 5 == 0 && reset == false) {
					apiindex++;
				}
				reset = false;

				JSONObject resp = sendGet(symbols[i], apikey[apiindex]);

				JSONObject arr = resp.getJSONObject("Monthly Adjusted Time Series");
				SortedMap<String, String> divMap = new TreeMap<String, String>(Collections.reverseOrder());
				Iterator it = arr.keys();
				while (it.hasNext()) {
					String key = (String) it.next();
					divMap.put(key, arr.getJSONObject(key).getString("7. dividend amount"));
				}
				// System.out.println(divMap.toString());

				boolean foundLastDiv = false;
				boolean growCheck = true;
				int growingDivYears = 0;
				BigDecimal prevYearDiv = BigDecimal.ZERO;
				Map<String, Object> yearsMap = new HashMap<String, Object>();
				String lastClosePrice = arr.getJSONObject(divMap.firstKey()).getString("5. adjusted close");
				for (String year : years) {
					Map<String, Object> yearMap = new HashMap<String, Object>();
					BigDecimal yearDiv = BigDecimal.ZERO;
					Map<String, BigDecimal> divDatas = new HashMap<String, BigDecimal>();
					for (String key : divMap.keySet()) {
						if (key.startsWith(year)) {
							BigDecimal dividend = new BigDecimal(divMap.get(key));
							if (dividend.compareTo(BigDecimal.ZERO) != 0) {
								yearDiv = yearDiv.add(dividend);
								divDatas.put(key, dividend);
							}
							if (foundLastDiv == false && dividend.compareTo(BigDecimal.ZERO) != 0) {
								foundLastDiv = true;
								yearDiv = dividend.divide(new BigDecimal(divRate[i]), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("12"));
								break;

							}
						}
					}
					if (year.compareTo(currentYear) == 0) {
						prevYearDiv = yearDiv;
					}
					if (year.compareTo(currentYear) != 0) {
						if (prevYearDiv.compareTo(yearDiv) >= 0 && growCheck == true) {
							growingDivYears++;
						} else {
							growCheck = false;
						}
					}

					BigDecimal yeld = yearDiv.divide(new BigDecimal(lastClosePrice), 4, RoundingMode.HALF_UP);
					System.out.println("symbol: " + symbols[i] + " year: " + year + " closePrice: " + lastClosePrice + " divDatas : " + divDatas.toString() + " divYear " + yearDiv + " yeld: " + yeld);
					prevYearDiv = yearDiv;
					yearMap.put("divDatas", divDatas);
					yearMap.put("divYear", yearDiv);
					yearMap.put("yield", yeld);
					yearsMap.put(year, yearMap);
				}

				stockMap.put("growDivYears", growingDivYears);
				stockMap.put("lastClosePrice", lastClosePrice);
				stockMap.put("yearsMap", yearsMap);
				stockMap.put("currentYear", currentYear);

				// System.out.println(" growDivYears: " + growingDivYears);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stocksMap.put(symbols[i], stockMap);

		}
		return stocksMap;

	}

	private JSONObject sendGet(String symbol, String apikey) throws Exception {

		String url = "https://www.alphavantage.co/query?function=TIME_SERIES_MONTHLY_ADJUSTED&symbol=" + symbol + "&apikey=" + apikey + "\"";

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
		// System.out.println(jsonObj.toString());
		// System.out.println("Symbol: " +
		// jsonObj.getJSONObject("Meta Data").get("2. Symbol"));
		return jsonObj;

	}

}
