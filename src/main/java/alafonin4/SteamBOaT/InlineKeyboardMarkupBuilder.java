package alafonin4.SteamBOaT;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardMarkupBuilder {
    public static InlineKeyboardMarkup setKeyboardForHistory(int orderIndex, int size) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var prButton = new InlineKeyboardButton();
        prButton.setText("Предыдущая страница");
        prButton.setCallbackData("Previous Page");

        var nButton = new InlineKeyboardButton();
        nButton.setText("Следующая страница");
        nButton.setCallbackData("Next Page");

        if (orderIndex - 3 + 1 > 0) {
            row.add(prButton);
        }

        if (orderIndex + 3 - 1 < size - 1) {
            row.add(nButton);
        }

        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }
    public static InlineKeyboardMarkup setKeyboard(List<Button> buttons) {
        InlineKeyboardMarkup marup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rList = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();

        for (var button:
             buttons) {
            var newButton = new InlineKeyboardButton();
            newButton.setText(button.getText());
            newButton.setCallbackData(button.getCallBack());
            if (button.getUrl() != null) {
                newButton.setUrl(button.getUrl());
            }
            r.add(newButton);
        }
        rList.add(r);
        marup.setKeyboard(rList);
        return marup;
    }
}
