package org.finos.symphony.toolkit.workflow;

import java.util.Map;

import org.finos.symphony.toolkit.workflow.fixture.WeirdObject;
import org.finos.springbot.sources.teams.content.TeamsUser;
import org.finos.springbot.sources.teams.conversations.TeamsConversations;
import org.finos.springbot.sources.teams.elements.FormConverter;
import org.finos.symphony.toolkit.workflow.fixture.TestObject;
import org.finos.symphony.toolkit.workflow.fixture.TestObjects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestFormConverter extends AbstractMockSymphonyTest{
	
	@MockBean
	TeamsConversations rooms;
	
	private FormConverter fc;
	private ObjectMapper om = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleForm() throws Exception {
		Object o = om.readValue("{\"action\":\"add+0\",\"isin.\":\"fd3442\",\"bidAxed.\":\"true\",\"askAxed.\":\"true\",\"creator.\":\"tr\",\"bidQty.\":\"32432\",\"askQty.\":\"234\"}", Map.class);
		TestObject to = (TestObject) fc.convert((Map<String, Object>) o, TestObject.class.getCanonicalName());
		Assertions.assertEquals("fd3442", to.getIsin());
		Assertions.assertEquals("tr", to.getCreator());
		Assertions.assertEquals(32432, to.getBidQty());
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFormContainingList() throws Exception {
		Object o = om.readValue("{\"action\":\"add+0\",\"items.[0].isin.\":\"fd3442\",\"items.[0].bidAxed.\":\"true\",\"items.[0].askAxed.\":\"true\",\"items.[0].creator.\":\"tr\",\"items.[0].bidQty.\":\"32432\",\"items.[0].askQty.\":\"234\"}", Map.class);
		TestObjects to = (TestObjects) fc.convert((Map<String, Object>) o, TestObjects.class.getCanonicalName());
		Assertions.assertEquals("fd3442", to.getItems().get(0).getIsin());
		Assertions.assertEquals("tr", to.getItems().get(0).getCreator());
		Assertions.assertEquals(32432, to.getItems().get(0).getBidQty());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUserDeserialize() throws Exception {
		before();
		Object o = om.readValue("{\"action\": \"ob4+0\", \"c.\": \"B\", \"b.\": true, \"someUser.\": [345315370602462]}", Map.class);
		WeirdObject to = (WeirdObject) fc.convert((Map<String, Object>) o, WeirdObject.class.getCanonicalName());
		Assertions.assertTrue(to.isB());
		Assertions.assertEquals("345315370602462", ((TeamsUser) to.getSomeUser()).getUserId());
		Assertions.assertEquals(WeirdObject.Choice.B, to.getC());
		
		
	}

	@Test
	public void testListOfStringAddValue() throws Exception {
		before();
		Object o = om.readValue("{\"action\": \"names.table-add-done\", \"entity.formdata\": \"Amsidh\"}", Map.class);
		@SuppressWarnings("unchecked")
		String to = (String) fc.convert((Map<String, Object>) o, String.class.getCanonicalName());
		Assertions.assertEquals("Amsidh", to);
	}


	@Test
	public void testListOfStringUpdateValue() throws Exception {
		before();
		Object o = om.readValue("{\"action\": \"names[0].table-update\", \"entity.formdata\": \"AmsidhLokhande\"}", Map.class);
		@SuppressWarnings("unchecked")
		String to = (String) fc.convert((Map<String, Object>) o, String.class.getCanonicalName());
		Assertions.assertEquals("AmsidhLokhande", to);
	}

	@Test
	public void testListOfIntegerAddValue() throws Exception {
		before();
		Object o = om.readValue("{\"action\": \"names.table-add-done\", \"entity.formdata\": 40}", Map.class);
		@SuppressWarnings("unchecked")
		Integer to = (Integer) fc.convert((Map<String, Object>) o, Integer.class.getCanonicalName());
		Assertions.assertEquals(40, to);
	}

	@BeforeEach
	public void before() {
		Mockito.when(rooms.loadUserById(Mockito.eq(345315370602462l))).thenReturn(new TeamsUser(345315370602462l, "Some Guy", "sg@example.com"));
		fc = new FormConverter(rooms);
	}
	
}
