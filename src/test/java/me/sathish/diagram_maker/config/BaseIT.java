package me.sathish.diagram_maker.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.path.json.config.JsonPathConfig;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import me.sathish.diagram_maker.DiagramMakerApplication;
import me.sathish.diagram_maker.service.FileDataService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import tools.jackson.databind.ObjectMapper;


/**
 * Abstract base class to be extended by every IT test, starting the Spring Boot context. A fresh Playwright page is available for each test.
 */
@SpringBootTest(
        classes = DiagramMakerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("it")
public abstract class BaseIT {

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    public int serverPort;

    @Autowired
    public ObjectMapper objectMapper;

    public BrowserContext context;

    public Page page;

    @Value("classpath:testFile.txt")
    public Resource testFile;

    @PostConstruct
    public void initRestAssured() {
        RestAssured.port = serverPort;
        RestAssured.urlEncodingEnabled = false;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.config = RestAssured.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE));
    }

    @BeforeAll
    public static void beforeAll() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    public void beforeEach() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL("http://localhost:" + serverPort));
        page = context.newPage();
    }

    @AfterEach
    public void afterEach() {
        context.close();
    }

    @AfterAll
    public static void afterAll() {
        browser.close();
        playwright.close();
    }

    @SneakyThrows
    public String readResource(final String resourceName) {
        return StreamUtils.copyToString(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public void prepareUpload(final String uid, final String fileName) {
        final File uploadFile = new File(FileDataService.UPLOAD_DIRECTORY + "/" + uid + "/" + fileName);
        uploadFile.getParentFile().mkdirs();
        uploadFile.createNewFile();
        FileCopyUtils.copy(testFile.getContentAsByteArray(), uploadFile);
    }

}
