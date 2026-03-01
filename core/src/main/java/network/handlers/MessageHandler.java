package network.handlers;

import com.watabou.utils.Bundle;

public interface MessageHandler {
    void msgHandle(int senderId, Bundle data);
    String getType(); // тип сообщения, который обрабатывает этот хендлер
}