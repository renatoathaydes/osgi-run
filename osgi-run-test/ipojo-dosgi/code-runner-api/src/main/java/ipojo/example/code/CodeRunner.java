package ipojo.example.code;

/**
 * Defines an interface for services that can run code in a single programming language.
 */
public interface CodeRunner {

    /**
     * @param script source code to run
     * @return the value returned by the script
     */
    Object runScript(String script);

    String getLanguage();

}
