package ipojo.example.code;

import javax.jws.WebParam;
import java.util.Set;

/**
 * Defines an interface for services that can run code in different programming languages.
 */
public interface CompositeCodeRunner {

    /**
     * @param language to use to interpret the script. Must be one of the languages returned
     *                 by {@link #getLanguages()}.
     * @param script   source code to run
     * @return the value returned by the script
     * @throws LanguageNotAvailableException if the language is not available.
     * @throws RemoteException               if an unknown Exception is thrown.
     */
    public String runScript(
            @WebParam(name = "language") String language,
            @WebParam(name = "script") String script)
            throws LanguageNotAvailableException, RemoteException;

    public Set<String> getLanguages();

}
