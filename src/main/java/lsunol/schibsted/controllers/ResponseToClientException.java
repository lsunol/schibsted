package lsunol.schibsted.controllers;

public class ResponseToClientException extends Exception {

    /**
     * HTTP response status that corresponds to the exception message.
     */
    int responseStatus;
    /**
     * Message that will be delivered to the HTTP client.
     */
    String messageToUser;
    /**
     * Name of the HTML template that will be used in case the HTTP client accepts a text/html format.
     */
    String templateToRender;

    public ResponseToClientException(int responseStatus, String messageToUser) {
        this.responseStatus = responseStatus;
        this.messageToUser = messageToUser;
    }

    public ResponseToClientException(int responseStatus, String messageToUser, String templateToRender) {
        this.responseStatus = responseStatus;
        this.messageToUser = messageToUser;
        this.templateToRender = templateToRender;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getMessageToUser() {
        return messageToUser;
    }

    public String getTemplateToRender() {
        if (templateToRender == null) return "error";
        else return templateToRender;
    }

    private ResponseToClientException() {
    }

    private ResponseToClientException(String message) {
        super(message);
    }

    private ResponseToClientException(String message, Throwable cause) {
        super(message, cause);
    }

    private ResponseToClientException(Throwable cause) {
        super(cause);
    }

    private ResponseToClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
