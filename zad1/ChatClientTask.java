/**
 * @author Śliwa Adam S25853
 */

package zad1;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<ChatClient> {

    public ChatClientTask(Callable<ChatClient> callable) {
        super(callable);
    }

    public static ChatClientTask create(ChatClient chatClient, List<String> messages, int wait) {
        return new ChatClientTask(() -> {
            try {
                if (wait != 0) Thread.sleep(wait);
                chatClient.login();
                for (String msg : messages) {
                    chatClient.send(msg);
                    if (wait != 0) Thread.sleep(wait);
                }
                chatClient.logout();
                if (wait != 0) Thread.sleep(wait);
                else Thread.sleep(30);
            } catch (InterruptedException e) {
                return null;
            }
            return chatClient;
        });
    }

    public ChatClient getClient() {
        try {
            return this.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }

    }
}
