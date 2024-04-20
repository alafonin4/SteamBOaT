package alafonin4.SteamBOaT;

public class Button {
    private final String text;
    private final String callBack;
    private final String url;

    public Button(String text, String callBack) {
        this.text = text;
        this.callBack = callBack;
        this.url = null;
    }

    public Button(String text, String callBack, String url) {
        this.text = text;
        this.callBack = callBack;
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public String getCallBack() {
        return callBack;
    }

    public String getUrl() {
        return url;
    }
}
