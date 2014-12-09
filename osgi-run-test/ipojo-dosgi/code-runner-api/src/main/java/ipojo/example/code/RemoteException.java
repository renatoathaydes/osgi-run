package ipojo.example.code;

/**
 * General Exception that can be thrown by any remote Service method invocation.
 * <p/>
 * Usually this Exception is thrown when an unexpected error occurs.
 */
public class RemoteException extends Exception {

    public RemoteException(String message) {
        super(message);
    }

}
