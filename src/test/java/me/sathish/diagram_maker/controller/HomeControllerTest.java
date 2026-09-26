package me.sathish.diagram_maker.controller;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import me.sathish.diagram_maker.config.BaseIT;
import org.junit.jupiter.api.Test;


public class HomeControllerTest extends BaseIT {

    @Test
    void getIndex_success() {
        page.navigate("/");
        assertThat(page.locator("h1")).hasText("Welcome to the Diagram Maker MCP");
    }

}
