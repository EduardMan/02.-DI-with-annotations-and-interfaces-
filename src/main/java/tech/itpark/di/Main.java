package tech.itpark.di;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        try {
            final var container = new Container();

            container.register("smsUrl", "https://sms.io");
            container.register("pushUrl", "https://firebase.io");
            container.register(SmsClient.class, PushClient.class, Service.class, RepositoryStubImpl.class);
            container.wire();

            System.out.println("finish");
        } catch (DIException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}

// 1. Откуда брать готовые значения: String, List<String>
// 2. Как различать параметры конструктора одинаковых типов
class SmsClient {
    private final String url;

    public SmsClient(@Inject("smsUrl") String url) {
        this.url = url;
    }
}

class PushClient {
    private final String url;

    public PushClient(@Inject("pushUrl") String url) {
        this.url = url;
    }
}

interface InjectableToMyService {

}

class RepositoryStubImpl implements Repository {
    @Override
    public List<Object> getAll() {
        return Collections.emptyList();
    }
}

class Service {
    private final Repository repository;

    public Service(Repository repository) {
        this.repository = repository;
    }
}
