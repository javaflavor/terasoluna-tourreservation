package org.terasoluna.tourreservation.app.it;

import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Configuration.*;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PageNavigationIT {
	static String targetHost = System.getProperty("target.host");
	static String targetPort = System.getProperty("target.port");
	static String targetBaseUrl = "http://"+targetHost+":"+targetPort+"/";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.printf("### target.host = %s%n", targetHost);
		System.out.printf("### target.port = %s%n", targetPort);

	    timeout = 10000;
	    baseUrl = targetBaseUrl;
	    startMaximized = false;

	    // Wait for application loaded.
	    TimeUnit.SECONDS.sleep(5);

		open(targetBaseUrl);
		// Wait for page loaded.
		$("h1").waitUntil(appears, 10000);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		closeWebDriver();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testShowTopPage() {
		open("/");

		// Tour Searchボタンが表示されていること。
		$("#searchTourBtn").shouldBe(visible);
		// Loginボタンが表示されていること。
		$("#loginBtn").shouldBe(visible);
		// Customer Registrationボタンが表示されていること。
		$("#customerRegisterBtn").shouldBe(visible);
	}
	
	@Test
	public void testShowSearchPage() {
		open("/");

		// Tour Searchボタンをクリック。
		$("#searchTourBtn").click();

		// 正常に表示。
		$("h2").shouldHave(text("Tour Search"));
		
		// タイトルリンクをクリック。
		$("#goToTopLink").click();
		
		// トップページが表示される。
		$("#messagesArea").shouldHave(text("This is a menu page."));
	}

	@Test
	public void testShowLoginPage() {
		open("/");

		// Loginボタンをクリック。
		$("#loginBtn").click();

		// 正常に表示。
		$("input[name=username]").shouldBe(visible);
		$("input[name=password]").shouldBe(visible);
		$("#loginBtn").shouldBe(visible);
		$("#resetBtn").shouldBe(visible);
		
		// タイトルリンクをクリック。
		$("#goToTopLink").click();
		
		// トップページが表示される。
		$("#messagesArea").shouldHave(text("This is a menu page."));
	}

	@Test
	public void testShowRegistrationPage() {
		open("/");

		// Customer Registrationボタンをクリック。
		$("#customerRegisterBtn").click();

		// 正常に表示。
		$("#customerForm").shouldBe(visible);
		$("#confirmBtn").shouldBe(visible);
		$("#resetBtn").shouldBe(visible);
		
		// タイトルリンクをクリック。
		$("#goToTopLink").click();
		
		// トップページが表示される。
		$("#messagesArea").shouldHave(text("This is a menu page."));
	}
}
