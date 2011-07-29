/*  Copyright 2008 Fabrizio Cannizzo
 *
 *  This file is part of RestFixture.
 *
 *  RestFixture (http://code.google.com/p/rest-fixture/) is free software:
 *  you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  RestFixture is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with RestFixture.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  If you want to contact the author please leave a comment here
 *  http://smartrics.blogspot.com/2008/08/get-fitnesse-with-some-rest.html
 */
package smartrics.rest.fitnesse.fixture;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import smartrics.rest.client.RestClient;
import smartrics.rest.client.RestData.Header;
import smartrics.rest.client.RestRequest;
import smartrics.rest.client.RestResponse;
import smartrics.rest.config.Config;
import smartrics.rest.fitnesse.fixture.support.BodyTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.CellFormatter;
import smartrics.rest.fitnesse.fixture.support.CellWrapper;
import smartrics.rest.fitnesse.fixture.support.ContentType;
import smartrics.rest.fitnesse.fixture.support.HeadersTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.JavascriptException;
import smartrics.rest.fitnesse.fixture.support.JavascriptWrapper;
import smartrics.rest.fitnesse.fixture.support.LetHandler;
import smartrics.rest.fitnesse.fixture.support.LetHandlerFactory;
import smartrics.rest.fitnesse.fixture.support.RestDataTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.RowWrapper;
import smartrics.rest.fitnesse.fixture.support.StatusCodeTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.StringTypeAdapter;
import smartrics.rest.fitnesse.fixture.support.Tools;
import smartrics.rest.fitnesse.fixture.support.Url;
import smartrics.rest.fitnesse.fixture.support.Variables;
import fit.ActionFixture;
import fit.Parse;

/**
 * A fixture that allows to simply test REST APIs with minimal efforts. The core
 * principles underpinning this fixture are:
 * <ul>
 * <li>allowing documentation of a REST API by showing how the API looks like.
 * For REST this means
 * <ul>
 * <li>show what the resource URI looks like. For example
 * <code>/resource-a/123/resource-b/234</code>
 * <li>show what HTTP operation is being executed on that resource. Specifically
 * which one fo the main HTTP verbs where under test (GET, POST, PUT, DELETE,
 * HEAD, OPTIONS).
 * <li>have the ability to set headers and body in the request
 * <li>check expectations on the return code of the call in order to document
 * the behaviour of the API
 * <li>check expectation on the HTTP headers and body in the response. Again, to
 * document the behaviour
 * </ul>
 * <li>should work without the need to write/maintain java code: tests are
 * written in wiki syntax.
 * <li>tests should be easy to write and above all read.
 * </ul>
 * 
 * <b>Configuring RestFixture</b><br/>
 * RestFixture can be configured by using the {@link RestFixtureConfig}. A
 * {@code RestFixtureConfig} can define named maps with configuration key/value
 * pairs. The name of the map is passed as second parameter to the
 * {@code RestFixture}. Using a named configuration is optional: if no name is
 * passed, the default configuration map is used. See {@link RestFixtureConfig}
 * for more details.
 * <p/>
 * The following list of configuration parameters can are supported.
 * <p/>
 * <table border="1">
 * <tr>
 * <td>smartrics.rest.fitnesse.fixture.RestFixtureConfig</td>
 * <td><i>optional named config</i></td>
 * </tr>
 * <tr>
 * <td>http.proxy.host</td>
 * <td><i>http proxy host name (RestClient proxy configutation)</i></td>
 * </tr>
 * <tr>
 * <td>http.proxy.port</td>
 * <td><i>http proxy host port (RestClient proxy configutation)</i></td>
 * </tr>
 * <tr>
 * <td>http.basicauth.username</td>
 * <td><i>username for basic authentication (RestClient proxy configutation)</i>
 * </td>
 * </tr>
 * <tr>
 * <td>http.basicauth.password</td>
 * <td><i>password for basic authentication (RestClient proxy configutation)</i>
 * </td>
 * </tr>
 * <tr>
 * <td>http.client.connection.timeout</td>
 * <td><i>client timeout for http connection (default 3s). (RestClient proxy
 * configutation)</i></td>
 * </tr>
 * <tr>
 * <td>restfixture.display.actual.on.right</td>
 * <td><i>boolean value. if true, the actual value of the header or body in an
 * expectation cell is displayed even when the expectation is met.</i></td>
 * </tr>
 * <tr>
 * <td>restfixture.default.headers</td>
 * <td><i>comma separated list of key value pairs representing the default list
 * of headers to be passed for each request. key and values are separated by a
 * colon. Entries are sepatated by \n. {@link RestFixture#setHeader()} will
 * override this value. </i></td>
 * </tr>
 * <tr>
 * <td>restfixture.xml.namespaces.context</td>
 * <td><i>comma separated list of key value pairs representing namespace
 * declarations. The key is the namespace alias, the value is the namespace URI.
 * alias and URI are separated by a = sign. Entries are sepatated by
 * {@code System.getProperty("line.separator")}. These entries will be used to
 * define the namespace context to be used in xpaths that are evaluated in the
 * results.</i></td>
 * </tr>
 * <tr>
 * <td>restfixture.content.handlers.map</td>
 * <td><i>a map of contenty type to type adapters, entries separated by \n, and
 * kye-value separated by '='. Available type adapters are JS, TEXT, JSON, XML
 * (see {@link smartrics.rest.fitnesse.fixture.support.BodyTypeAdapterFactory}
 * ).</i></td>
 * </tr>
 * <tr>
 * <td>restfixture.null.value.representation</td>
 * <td><i>This string is used in replacement of the default string substituted
 * when a null value is set for a symbol. Because now the RestFixture labels
 * support is implemented on top of the Fitnesse symbols, such default value is
 * defined in Fitnesse, and that is the string 'null'. Hence, every substitution
 * that would result in rendering the string 'null' is replaced with the value
 * set for this config key. This value can also be the empty string to replace
 * null with empty.</i></td>
 * </tr>
 * 
 * </table>
 * 
 * @author smartrics
 */
public class RestFixture extends ActionFixture {

    /**
     * What runner this table is running on.
     * 
     * Note, the OTHER runner is primarily for testing purposes.
     * 
     * @author fabrizio
     * 
     */
    enum Runner {
        SLIM, FIT, OTHER;
    };

    private static final String LINE_SEPARATOR = "\n";

    private static final String FILE = "file";

    private static final Log LOG = LogFactory.getLog(RestFixture.class);

    protected Variables GLOBALS;

    private RestResponse lastResponse;

    private RestRequest lastRequest;

    private String fileName = null;

    private String multipartFileName = null;

    private String multipartFileParameterName = FILE;

    private String requestBody;

    private Map<String, String> requestHeaders;

    private RestClient restClient;

    private Config config;

    private boolean displayActualOnRight;

    private boolean debugMethodCall = false;

    /**
     * the headers passed to each request by default.
     */
    private Map<String, String> defaultHeaders = new HashMap<String, String>();

    private Map<String, String> namespaceContext = new HashMap<String, String>();

    private Url baseUrl;

    @SuppressWarnings("rawtypes")
    protected RowWrapper row;

    private CellFormatter<?> formatter;

    private PartsFactory partsFactory;

    private String lastEvaluation;

    private int minLenForCollapseToggle;

    /**
     * Constructor for Fit runner.
     */
    public RestFixture() {
        super();
        this.partsFactory = new PartsFactory();
        this.displayActualOnRight = true;
        this.minLenForCollapseToggle = -1;
    }

    /**
     * Constructor for Slim runner.
     * 
     * @param args
     *            the cells following up the first cell in the first row.
     */
    public RestFixture(String hostName) {
        this(hostName, Config.DEFAULT_CONFIG_NAME);
    }

    /**
     * Constructor for Slim runner.
     * 
     * @param args
     *            the cells following up the first cell in the first row.
     */
    public RestFixture(String hostName, String configName) {
        this(new PartsFactory(), hostName, configName);
    }

    RestFixture(PartsFactory partsFactory, String hostName) {
        this(partsFactory, hostName, Config.DEFAULT_CONFIG_NAME);
    }

    RestFixture(PartsFactory partsFactory, String hostName, String configName) {
        this.displayActualOnRight = true;
        this.minLenForCollapseToggle = -1;
        this.partsFactory = partsFactory;
        this.config = Config.getConfig(configName);
        this.baseUrl = new Url(stripTag(hostName));
    }

    /**
     * @return the config used for this fixture instance
     */
    public Config getConfig() {
        return config;
    }

    /**
     * @return the result of the last evaluation performed via evalJs.
     */
    public String getLastEvaluation() {
        return lastEvaluation;
    }

    /**
     * The base URL as defined by the rest fixture ctor or input args.
     * 
     * @return the base URL as string
     */
    public String getBaseUrl() {
        if (baseUrl != null) {
            return baseUrl.toString();
        }
        return null;
    }

    /**
     * The default headers as defined in the config used to initialise this
     * fixture.
     * 
     * @return the map of default headers.
     */
    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    /**
     * The formatter for this instance of the RestFixture.
     * 
     * @return
     */
    public CellFormatter<?> getFormatter() {
        return formatter;
    }

    /**
     * Slim Table table hook.
     * 
     * @param rows
     * @return
     */
    public List<List<String>> doTable(List<List<String>> rows) {
        initialize(Runner.SLIM);
        List<List<String>> res = new Vector<List<String>>();
        getFormatter().setDisplayActual(displayActualOnRight);
        getFormatter().setMinLenghtForToggleCollapse(minLenForCollapseToggle);
        for (List<String> r : rows) {
            processSlimRow(res, r);
        }
        return res;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void doCells(Parse parse) {
        config = Config.getConfig(getConfigNameFromArgs());
        String url = getBaseUrlFromArgs();
        if (url != null) {
            baseUrl = new Url(stripTag(url));
        }
        initialize(Runner.FIT);
        getFormatter().setDisplayActual(displayActualOnRight);
        getFormatter().setMinLenghtForToggleCollapse(minLenForCollapseToggle);
        ((FitFormatter) getFormatter()).setActionFixtureDelegate(this);
        RowWrapper currentRow = new FitRow(parse);
        try {
            processRow(currentRow);
        } catch (Exception exception) {
            getFormatter().exception(currentRow.getCell(0), exception);
        }
    }

    /**
     * Process args to extract the optional config name.
     * 
     * @return
     */
    protected String getConfigNameFromArgs() {
        if (args.length >= 2) {
            return args[1];
        }
        return null;
    }

    /**
     * Process args ({@see fit.Fixture}) for Fit runner to extract the baseUrl
     * of each Rest request, first parameter of each RestFixture table.
     * 
     * @return
     */
    protected String getBaseUrlFromArgs() {
        if (args.length > 0) {
            return args[0];
        }
        return null;
    }

    /**
     * Overrideable method to validate the state of the instance in execution. A
     * {@link RestFixture} is valid if the baseUrl is not null.
     * 
     * @return true if the state is valid, false otherwise
     */
    protected boolean validateState() {
        return baseUrl != null;
    }

    protected void setConfig(Config c) {
        this.config = c;
    }

    /**
     * Method invoked to notify that the state of the RestFixture is invalid. It
     * throws a {@link RuntimeException} with a message displayed in the
     * FitNesse page.
     * 
     * @param state
     *            as returned by {@link RestFixture#validateState()}
     */
    protected void notifyInvalidState(boolean state) {
        if (!state) {
            throw new RuntimeException("You must specify a base url in the |start|, after the fixture to start");
        }
    }

    /**
     * Allows setting of the name of the multi-part file to upload.
     * 
     * <code>| setMultipartFileName | Name of file |</code>
     * <p/>
     * body text should be location of file which needs to be sent
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setMultipartFileName() {
        CellWrapper cell = row.getCell(1);
        if (cell == null) {
            getFormatter().exception(row.getCell(0), "You must pass a multipart file name to set");
        } else {
            multipartFileName = GLOBALS.substitute(cell.text());
            renderReplacement(cell, multipartFileName);
        }
    }

    public String getMultipartFileName() {
        return multipartFileName;
    }

    /**
     * Allows setting of the name of the file to upload.
     * 
     * <code>| setFileName | Name of file |</code>
     * <p/>
     * body text should be location of file which needs to be sent
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setFileName() {
        CellWrapper cell = row.getCell(1);
        if (cell == null) {
            getFormatter().exception(row.getCell(0), "You must pass a file name to set");
        } else {
            fileName = GLOBALS.substitute(cell.text());
            renderReplacement(cell, fileName);
        }
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the parameter to send in the request storing the multi-part file to
     * upload. If not specified the default is <code>file</code>
     * <p/>
     * <code>| setMultipartFileParameterName | Name of form parameter for the uploaded file |</code>
     * <p/>
     * body text should be the name of the form parameter, defaults to 'file'
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setMultipartFileParameterName() {
        CellWrapper cell = row.getCell(1);
        if (cell == null) {
            getFormatter().exception(row.getCell(0), "You must pass a parameter name to set");
        } else {
            multipartFileParameterName = GLOBALS.substitute(cell.text());
            renderReplacement(cell, multipartFileParameterName);
        }
    }

    public String getMultipartFileParameterName() {
        return multipartFileParameterName;
    }

    /**
     * <code>| setBody | body text goes here |</code>
     * <p/>
     * body text can either be a kvp or a xml. The <code>ClientHelper</code>
     * will figure it out
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setBody() {
        CellWrapper cell = row.getCell(1);
        if (cell == null) {
            getFormatter().exception(row.getCell(0), "You must pass a body to set");
        } else {
            String text = getFormatter().fromRaw(cell.text());
            requestBody = GLOBALS.substitute(text);
            renderReplacement(cell, requestBody);
        }
    }

    /**
     * <code>| setHeader | http headers go here as nvp |</code>
     * <p/>
     * header text must be nvp. name and value must be separated by ':' and each
     * header is in its own line
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setHeader() {
        CellWrapper cell = row.getCell(1);
        if (cell == null) {
            getFormatter().exception(row.getCell(0), "You must pass a header map to set");
        } else {
            String header = GLOBALS.substitute(cell.text());
            requestHeaders = parseHeaders(header);
        }
    }

    /**
     * Equivalent to setHeader - syntactic sugar to indicate that you can now.
     * 
     * set multiple headers in a single call
     */
    public void setHeaders() {
        setHeader();
    }

    /**
     * <code> | PUT | URL | ?ret | ?headers | ?body |</code>
     * <p/>
     * executes a PUT on the URL and checks the return (a string representation
     * the operation return code), the HTTP response headers and the HTTP
     * response body
     * 
     * URL is resolved by replacing global variables previously defined with
     * <code>let()</code>
     * 
     * the HTTP request headers can be set via <code>setHeaders()</code>. If not
     * set, the list of default headers will be set. See
     * <code>DEF_REQUEST_HEADERS</code>
     */
    public void PUT() {
        debugMethodCallStart();
        doMethod(emptifyBody(requestBody), "Put");
        debugMethodCallEnd();
    }

    /**
     * <code> | GET | uri | ?ret | ?headers | ?body |</code>
     * <p/>
     * executes a GET on the uri and checks the return (a string repr the
     * operation return code), the http response headers and the http response
     * body
     * 
     * uri is resolved by replacing vars previously defined with
     * <code>let()</code>
     * 
     * the http request headers can be set via <code>setHeaders()</code>. If not
     * set, the list of default headers will be set. See
     * <code>DEF_REQUEST_HEADERS</code>
     */
    public void GET() {
        debugMethodCallStart();
        doMethod("Get");
        debugMethodCallEnd();
    }

    /**
     * <code> | DELETE | uri | ?ret | ?headers | ?body |</code>
     * <p/>
     * executes a DELETE on the uri and checks the return (a string repr the
     * operation return code), the http response headers and the http response
     * body
     * 
     * uri is resolved by replacing vars previously defined with
     * <code>let()</code>
     * 
     * the http request headers can be set via <code>setHeaders()</code>. If not
     * set, the list of default headers will be set. See
     * <code>DEF_REQUEST_HEADERS</code>
     */
    public void DELETE() {
        debugMethodCallStart();
        doMethod("Delete");
        debugMethodCallEnd();
    }

    /**
     * <code> | POST | uri | ?ret | ?headers | ?body |</code>
     * <p/>
     * executes a POST on the uri and checks the return (a string repr the
     * operation return code), the http response headers and the http response
     * body
     * 
     * uri is resolved by replacing vars previously defined with
     * <code>let()</code>
     * 
     * post requires a body that can be set via <code>setBody()</code>.
     * 
     * the http request headers can be set via <code>setHeaders()</code>. If not
     * set, the list of default headers will be set. See
     * <code>DEF_REQUEST_HEADERS</code>
     */
    public void POST() {
        debugMethodCallStart();
        doMethod(emptifyBody(requestBody), "Post");
        debugMethodCallEnd();
    }

    /**
     * <code> | let | label | type | loc | expr |</code>
     * <p/>
     * allows to associate a value to a label. values are extracted from the
     * body of the last successful http response.
     * <ul>
     * <li/><code>label</code> is the label identifier
     * 
     * <li/><code>type</code> is the type of operation to perform on the last
     * http response. At the moment only XPaths and Regexes are supported. In
     * case of regular expressions, the expression must contain only one group
     * match, if multiple groups are matched the label will be assigned to the
     * first found <code>type</code> only allowed values are <code>xpath</code>
     * and <code>regex</code>
     * 
     * <li/><code>loc</code> where to apply the <code>expr</code> of the given
     * <code>type</code>. Currently only <code>header</code> and
     * <code>body</code> are supported. If type is <code>xpath</code> by default
     * the expression is matched against the body and the value in loc is
     * ignored.
     * 
     * <li/><code>expr</code> is the expression of type <code>type</code> to be
     * executed on the last http response to extract the content to be
     * associated to the label.
     * </ul>
     * <p/>
     * <code>label</code>s can be retrieved after they have been defined and
     * their scope is the fixture instance under execution. They are stored in a
     * map so multiple calls to <code>let()</code> with the same label will
     * override the current value of that label.
     * <p/>
     * Labels are resolved in <code>uri</code>s, <code>header</code>s and
     * <code>body</code>es.
     * <p/>
     * In order to be resolved a label must be between <code>%</code>, e.g.
     * <code>%id%</code>.
     * <p/>
     * The test row must have an empy cell at the end that will display the
     * value extracted and assigned to the label.
     * <p/>
     * Example: <br/>
     * <code>| GET | /services | 200 | | |</code><br/>
     * <code>| let | id |  body | /services/id[0]/text() | |</code><br/>
     * <code>| GET | /services/%id% | 200 | | |</code>
     * <p/>
     * or
     * <p/>
     * <code>| POST | /services | 201 | | |</code><br/>
     * <code>| let  | id | header | /services/([.]+) | |</code><br/>
     * <code>| GET  | /services/%id% | 200 | | |</code>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void let() {
        debugMethodCallStart();
        String label = row.getCell(1).text().trim();
        String loc = row.getCell(2).text();
        CellWrapper exprCell = row.getCell(3);
        exprCell.body(GLOBALS.substitute(exprCell.body()));
        String expr = exprCell.text();
        CellWrapper valueCell = row.getCell(4);
        String valueCellText = valueCell.body();
        String valueCellTextReplaced = GLOBALS.substitute(valueCellText);
        valueCell.body(valueCellTextReplaced);
        String sValue = null;
        try {
            LetHandler letHandler = LetHandlerFactory.getHandlerFor(loc);
            if (letHandler != null) {
                StringTypeAdapter adapter = new StringTypeAdapter();
                try {
                    sValue = letHandler.handle(getLastResponse(), namespaceContext, expr);
                    exprCell.body(getFormatter().gray(exprCell.body()));
                } catch (RuntimeException e) {
                    getFormatter().exception(exprCell, e.getMessage());
                }
                GLOBALS.put(label, sValue);
                adapter.set(sValue);
                getFormatter().check(valueCell, adapter);
            } else {
                getFormatter().exception(exprCell, "I don't know how to process the expression for '" + loc + "'");
            }
        } catch (RuntimeException e) {
            getFormatter().exception(exprCell, e);
        } finally {
            debugMethodCallEnd();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void comment() {
        debugMethodCallStart();
        CellWrapper messageCell = row.getCell(1);
        try {
            String message = messageCell.text().trim();
            message = GLOBALS.substitute(message);
            messageCell.body(getFormatter().gray(message));
        } catch (RuntimeException e) {
            getFormatter().exception(messageCell, e);
        } finally {
            debugMethodCallEnd();
        }
    }

    /**
     * Evaluates a string using the internal JavaScript engine. Result of the
     * last evaluation is set in the lastEvaluation field.
     * 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void evalJs() {
        CellWrapper jsCell = row.getCell(1);
        if (jsCell == null) {
            getFormatter().exception(row.getCell(0), "Missing string to evaluate)");
            return;
        }
        JavascriptWrapper wrapper = new JavascriptWrapper();
        Object result = null;
        try {
            result = wrapper.evaluateExpression(lastResponse, jsCell.body());
        } catch (JavascriptException e) {
            getFormatter().exception(row.getCell(1), e);
            return;
        }
        lastEvaluation = null;
        if (result != null) {
            lastEvaluation = result.toString();
        }
        StringTypeAdapter adapter = new StringTypeAdapter();
        adapter.set(lastEvaluation);
        getFormatter().right(row.getCell(1), adapter);
    }

    /**
     * Process the row in input. Abstracts the test runner via the wrapper
     * interfaces.
     * 
     * @param currentRow
     */
    @SuppressWarnings("rawtypes")
    public void processRow(RowWrapper<?> currentRow) {
        row = currentRow;
        CellWrapper cell0 = row.getCell(0);
        if (cell0 == null) {
            throw new RuntimeException("Current RestFixture row is not parseable (maybe empty or not existent)");
        }
        String methodName = cell0.text();
        if ("".equals(methodName)) {
            throw new RuntimeException("RestFixture method not specified");
        }
        Method method1 = null;
        try {
            method1 = getClass().getMethod(methodName);
            method1.invoke(this);
        } catch (SecurityException e) {
            throw new RuntimeException("Not enough permissions to access method " + methodName + " for this class " + this.getClass().getSimpleName(), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class " + this.getClass().getName() + " doesn't have a callable method named " + methodName, e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Method named " + methodName + " invoked with the wrong argument.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Method named " + methodName + " is not public.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Method named " + methodName + " threw an exception when executing.", e);
        }
    }

    protected void initialize(Runner runner) {
        boolean state = validateState();
        notifyInvalidState(state);
        configFormatter(runner);
        configFixture();
        configRestClient();
    }

    private String emptifyBody(String b) {
        String body = b;
        if (body == null) {
            body = "";
        }
        return body;
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = null;
        if (requestHeaders != null) {
            headers = requestHeaders;
        } else {
            headers = defaultHeaders;
        }
        return headers;
    }

    private void doMethod(String m) {
        doMethod(null, m);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void doMethod(String body, String method) {
        CellWrapper urlCell = row.getCell(1);
        String url = urlCell.text();
        String resUrl = GLOBALS.substitute(url);
        setLastRequest(partsFactory.buildRestRequest());
        getLastRequest().setMethod(RestRequest.Method.valueOf(method));
        getLastRequest().addHeaders(getHeaders());
        if (fileName != null) {
            getLastRequest().setFileName(fileName);
        }
        if (multipartFileName != null) {
            getLastRequest().setMultipartFileName(multipartFileName);
        }
        getLastRequest().setMultipartFileParameterName(multipartFileParameterName);
        String[] uri = resUrl.split("\\?");
        getLastRequest().setResource(uri[0]);
        if (uri.length == 2) {
            getLastRequest().setQuery(uri[1]);
        }
        if ("Post".equals(method) || "Put".equals(method)) {
            String rBody = GLOBALS.substitute(body);
            getLastRequest().setBody(rBody);
        }
        try {
            RestResponse response = restClient.execute(getLastRequest());
            setLastResponse(response);
            completeHttpMethodExecution();
        } catch (RuntimeException e) {
            getFormatter().exception(row.getCell(0), "Execution of " + method + " caused exception '" + e.getMessage() + "'");
        }
    }

    private ContentType getContentTypeOfLastResponse() {
        return ContentType.parse(getLastResponse().getHeader("Content-Type"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void completeHttpMethodExecution() {
        String uri = getLastResponse().getResource();
        String query = getLastRequest().getQuery();
        if (query != null && !"".equals(query.trim())) {
            uri = uri + "?" + query;
        }
        String u = restClient.getBaseUrl() + uri;
        CellWrapper uriCell = row.getCell(1);
        getFormatter().asLink(uriCell, u, uri);
        CellWrapper cellStatusCode = row.getCell(2);
        Integer lastStatusCode = getLastResponse().getStatusCode();
        process(cellStatusCode, lastStatusCode.toString(), new StatusCodeTypeAdapter());
        List<Header> lastHeaders = getLastResponse().getHeaders();
        process(row.getCell(3), lastHeaders, new HeadersTypeAdapter());
        CellWrapper bodyCell = row.getCell(4);
        bodyCell.body(GLOBALS.substitute(bodyCell.body()));
        ContentType ct = getContentTypeOfLastResponse();
        BodyTypeAdapter bodyTypeAdapter = partsFactory.buildBodyTypeAdapter(ct);
        bodyTypeAdapter.setContext(namespaceContext);
        process(bodyCell, getLastResponse().getBody(), bodyTypeAdapter);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void process(CellWrapper expected, Object actual, RestDataTypeAdapter ta) {
        ta.set(actual);
        boolean ignore = "".equals(expected.text().trim());
        if (ignore) {
            String actualString = ta.toString();
            if (!"".equals(actualString)) {
                expected.addToBody(getFormatter().gray(actualString));
            }
        } else {
            boolean success = false;
            try {
                String substitute = GLOBALS.substitute(Tools.fromHtml(expected.text()));
                Object parse = ta.parse(substitute);
                success = ta.equals(parse, actual);
            } catch (Exception e) {
                getFormatter().exception(expected, e);
                return;
            }
            if (success) {
                getFormatter().right(expected, ta);
            } else {
                getFormatter().wrong(expected, ta);
            }
        }
    }

    private void debugMethodCallStart() {
        debugMethodCall("=> ");
    }

    private void debugMethodCallEnd() {
        debugMethodCall("<= ");
    }

    private void debugMethodCall(String h) {
        if (debugMethodCall) {
            StackTraceElement el = Thread.currentThread().getStackTrace()[4];
            LOG.debug(h + el.getMethodName());
        }
    }

    protected RestResponse getLastResponse() {
        return lastResponse;
    }

    protected RestRequest getLastRequest() {
        return lastRequest;
    }

    private void setLastResponse(RestResponse lastResponse) {
        this.lastResponse = lastResponse;
    }

    private void setLastRequest(RestRequest lastRequest) {
        this.lastRequest = lastRequest;
    }

    private Map<String, String> parseHeaders(String str) {
        return Tools.convertStringToMap(str, ":", LINE_SEPARATOR);
    }

    private Map<String, String> parseNamespaceContext(String str) {
        return Tools.convertStringToMap(str, "=", LINE_SEPARATOR);
    }

    private String stripTag(String somethingWithinATag) {
        return Tools.fromSimpleTag(somethingWithinATag);
    }

    private void configFormatter(Runner runner) {
        formatter = partsFactory.buildCellFormatter(runner);
    }

    /**
     * Configure the fixture with data from {@link RestFixtureConfig}.
     */
    private void configFixture() {

        GLOBALS = new Variables(config);

        displayActualOnRight = config.getAsBoolean("restfixture.display.actual.on.right", displayActualOnRight);

        minLenForCollapseToggle = config.getAsInteger("restfixture.display.toggle.for.cells.larger.than", minLenForCollapseToggle);

        String str = config.get("restfixture.default.headers", "");
        defaultHeaders = parseHeaders(str);

        str = config.get("restfixture.xml.namespace.context", "");
        namespaceContext = parseNamespaceContext(str);

        ContentType.resetDefaultMapping();
        ContentType.config(config);
    }

    /**
     * Allows to config the rest client implementation. the method shoudl
     * configure the instance attribute {@link RestFixture#restClient} created
     * by the {@link RestFixture#buildRestClient()}.
     */
    private void configRestClient() {
        restClient = partsFactory.buildRestClient(getConfig());
        if (baseUrl != null) {
            restClient.setBaseUrl(baseUrl.toString());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void renderReplacement(CellWrapper cell, String actual) {
        StringTypeAdapter adapter = new StringTypeAdapter();
        adapter.set(actual);
        if (!adapter.equals(actual, cell.body())) {
            // eg - a substitution has occurred
            getFormatter().right(cell, adapter);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processSlimRow(List<List<String>> resultTable, List<String> row) {
        RowWrapper currentRow = new SlimRow(row);
        try {
            processRow(currentRow);
        } catch (Exception e) {
            LOG.error("Exception raised when executing method " + row.get(0));
            getFormatter().exception(currentRow.getCell(0), e);
        } finally {
            List<String> rowAsList = mapSlimRow(row, currentRow);
            resultTable.add(rowAsList);
        }
    }

    @SuppressWarnings("rawtypes")
    private List<String> mapSlimRow(List<String> resultRow, RowWrapper currentRow) {
        List<String> rowAsList = ((SlimRow) currentRow).asList();
        for (int c = 0; c < rowAsList.size(); c++) {
            // HACK: it seems that even if the content is unchanged,
            // Slim renders red cell
            String v = rowAsList.get(c);
            if (v.equals(resultRow.get(c))) {
                rowAsList.set(c, "");
            }
        }
        return rowAsList;
    }
}
