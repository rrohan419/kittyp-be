package com.kittyp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "management.server.port=0")
class KittypApplicationTests {

	@Test
	void contextLoads() {
	}

}
