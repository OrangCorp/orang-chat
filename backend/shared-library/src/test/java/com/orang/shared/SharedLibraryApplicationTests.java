package com.orang.shared;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SharedLibraryApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainMethodRuns() {
		SharedLibraryApplication.main(new String[]{"--spring.main.web-application-type=none"});
	}

}
