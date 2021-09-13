package org.finos.symphony.toolkit.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.finos.symphony.toolkit.json.EntityJson;
import org.finos.symphony.toolkit.workflow.actions.Action;
import org.finos.symphony.toolkit.workflow.actions.SimpleMessageAction;
import org.finos.symphony.toolkit.workflow.annotations.ChatRequest;
import org.finos.symphony.toolkit.workflow.content.Chat;
import org.finos.symphony.toolkit.workflow.content.CodeBlock;
import org.finos.symphony.toolkit.workflow.content.Message;
import org.finos.symphony.toolkit.workflow.content.Paragraph;
import org.finos.symphony.toolkit.workflow.content.Table;
import org.finos.symphony.toolkit.workflow.content.User;
import org.finos.symphony.toolkit.workflow.content.Word;
import org.finos.symphony.toolkit.workflow.fixture.OurController;
import org.finos.symphony.toolkit.workflow.form.ButtonList;
import org.finos.symphony.toolkit.workflow.java.mapping.ChatHandlerMappingActionConsumer;
import org.finos.symphony.toolkit.workflow.java.mapping.ChatMapping;
import org.finos.symphony.toolkit.workflow.java.mapping.ChatRequestChatHandlerMapping;
import org.finos.symphony.toolkit.workflow.response.ErrorResponse;
import org.finos.symphony.toolkit.workflow.response.WorkResponse;
import org.finos.symphony.toolkit.workflow.sources.symphony.SymphonyWorkflowConfig;
import org.finos.symphony.toolkit.workflow.sources.symphony.content.HashTag;
import org.finos.symphony.toolkit.workflow.sources.symphony.content.SymphonyRoom;
import org.finos.symphony.toolkit.workflow.sources.symphony.content.SymphonyUser;
import org.finos.symphony.toolkit.workflow.sources.symphony.handlers.SymphonyResponseHandler;
import org.finos.symphony.toolkit.workflow.sources.symphony.json.EntityJsonConverter;
import org.finos.symphony.toolkit.workflow.sources.symphony.messages.MessageMLParser;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest(classes = {
		AbstractMockSymphonyTest.MockConfiguration.class, 
		SymphonyWorkflowConfig.class,
})
@ExtendWith(SpringExtension.class)
public class TestHandlerMapping extends AbstractMockSymphonyTest {

	@Autowired
	OurController oc;
	
	@Autowired
	ChatRequestChatHandlerMapping hm;
		
	@Autowired
	SymphonyResponseHandler rh;
	
	@Autowired
	ChatHandlerMappingActionConsumer mc;
	
	@Autowired
	EntityJsonConverter ejc;
	
	@Autowired
	MessageMLParser smp;

	@Test
	public void checkMappings() throws Exception {
		Assertions.assertEquals(15, hm.getHandlerMethods().size());
		getMappingsFor("list");
	}

	private List<ChatMapping<ChatRequest>> getMappingsFor(String s) throws Exception {
		EntityJson jsonObjects = new EntityJson();
		Message m = smp.parse("<messageML>"+s+"</messageML>", jsonObjects);
		Action a = new SimpleMessageAction(null, null, m, jsonObjects);
		return hm.getHandlers(a);
	}
	

	private void execute(String s) throws Exception {
		EntityJson jsonObjects = new EntityJson();
		jsonObjects.put("1", new SymphonyUser(123l, "gaurav", "gaurav@example.com"));
		jsonObjects.put("2", new HashTag("SomeTopic"));
		Message m = smp.parse("<messageML>/"+s+"</messageML>", jsonObjects);
		Chat r = new SymphonyRoom("The Room Where It Happened", "abc123");
		User author = new SymphonyUser(ROB_EXAMPLE_ID, ROB_NAME, ROB_EXAMPLE_EMAIL);
		Action a = new SimpleMessageAction(r, author, m, jsonObjects);
		Action.CURRENT_ACTION.set(a);
		mc.accept(a);
	}
	
	@Test
	public void checkWildcardMapping() throws Exception {
		List<ChatMapping<ChatRequest>> mapped = getMappingsFor("ban zebedee");
		Assertions.assertTrue(mapped.size()  == 1);
	}
	
	@Test
	public void checkHandlerExecutors() throws Exception {
		execute("ban zebedee");
		Assertions.assertEquals("banWord", oc.lastMethod);
		Assertions.assertEquals(1,  oc.lastArguments.size());
		Assertions.assertEquals(Word.of("zebedee"),oc.lastArguments.get(0));
	}
	
	@Test
	public void checkMethodCall() throws Exception {
		execute("list");
		Assertions.assertEquals("doCommand", oc.lastMethod);
		Assertions.assertEquals(1,  oc.lastArguments.size());
		Assertions.assertTrue(Message.class.isAssignableFrom(oc.lastArguments.get(0).getClass()));
		
	}
	
	
	@Test
	public void checkMethodCallWithChatVariables() throws Exception {
		execute("ban gaurav");
		Assertions.assertEquals("banWord", oc.lastMethod);
		Assertions.assertEquals(1,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(Word.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("gaurav", ((Word)firstArgument).getText());
	}
	
	@Test
	public void testAuthorChatVariable() throws Exception {
		execute("userDetails2 <span class=\"entity\" data-entity-id=\"1\">@gaurav</span>");
		Assertions.assertEquals("userDetails2", oc.lastMethod);
		Assertions.assertEquals(2,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(User.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("gaurav", ((User)firstArgument).getName());
		
		Object secondArgument = oc.lastArguments.get(1);
		Assertions.assertTrue(User.class.isAssignableFrom(secondArgument.getClass()));
		Assertions.assertEquals(ROB_EXAMPLE_EMAIL, ((User)secondArgument).getEmailAddress());
	}
	
	@Test
	public void testUserChatVariable() throws Exception {
		execute("delete <span class=\"entity\" data-entity-id=\"1\">@gaurav</span>");
		Assertions.assertEquals("removeUserFromRoom", oc.lastMethod);
		Assertions.assertEquals(2,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(User.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("gaurav", ((User)firstArgument).getName());
		
		Object secondArgument = oc.lastArguments.get(1);
		Assertions.assertTrue(Chat.class.isAssignableFrom(secondArgument.getClass()));
		Assertions.assertEquals("The Room Where It Happened", ((Chat)secondArgument).getName());
	}
	
	@Test
	public void testHashtagMapping() throws Exception {
		execute("add <span class=\"entity\" data-entity-id=\"1\">@gaurav</span> to <span class=\"entity\" data-entity-id=\"2\">#SomeTopic</span>");
		Assertions.assertEquals("addUserToTopic", oc.lastMethod);
		Assertions.assertEquals(2,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(User.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("gaurav", ((User)firstArgument).getName());
		
		Object secondArgument = oc.lastArguments.get(1);
		Assertions.assertTrue(HashTag.class.isAssignableFrom(secondArgument.getClass()));
		Assertions.assertEquals("SomeTopic", ((HashTag)secondArgument).getName());
	}
	
	
	@Test
	public void testCodeblockMapping() throws Exception {
		execute("update <pre>public static void main(String[] args) {}</pre>");
		Assertions.assertEquals("process2", oc.lastMethod);
		Assertions.assertEquals(1,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(CodeBlock.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("public static void main(String[] args) {}", ((CodeBlock)firstArgument).getText());
	}
	
	@Test
	public void testHelp() throws Exception {
		execute("help");
		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
		
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
				Mockito.isNull(), 
				Mockito.matches("abc123"),
				msg.capture(),
				data.capture(),
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull());
		
		JsonNode node = new ObjectMapper().readTree(data.getValue());
		System.out.println(msg.getValue());
		System.out.println(data.getValue());
		
		
		Assertions.assertEquals(14, node.get(WorkResponse.OBJECT_KEY).get("commands").size());
		
		Assertions.assertTrue(data.getValue().contains(" {\n"
				+ "      \"type\" : \"org.finos.symphony.toolkit.workflow.help.commandDescription\",\n"
				+ "      \"version\" : \"1.0\",\n"
				+ "      \"description\" : \"Display this help page\",\n"
				+ "      \"examples\" : [ \"help\" ]\n"
				+ "    }"));
		
		Assertions.assertTrue(msg.getValue().contains("<tr>\n"
				+ "            <th>Description</th>\n"
				+ "            <th>Type... </th>\n"
				+ "          </tr>"));

	}
	

	@Test
	public void testCodeblockMapping2() throws Exception {
		execute("update <code>public <a href=\"sfdkjfh\">nonsense</a>static void main(String[] args) {}</code>");
		Assertions.assertEquals("process2", oc.lastMethod);
		Assertions.assertEquals(1,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(CodeBlock.class.isAssignableFrom(firstArgument.getClass()));
		Assertions.assertEquals("public static void main(String[] args) {}", ((CodeBlock)firstArgument).getText());
	}
	
	

	@Test
	public void testTableMapping() throws Exception {
		execute("process-table <table>\n"
				+ "      <tr>\n"
				+ "        <th>Thing</th><th>Thang</th>\n"
				+ "      </tr>\n"
				+ "      <tr>\n"
				+ "        <td>1</td><td>2</td>\n"
				+ "      </tr>\n"
				+ "      <tr>\n"
				+ "        <td>3</td><td>4</td>\n"
				+ "      </tr>\n"
				+ "  </table> <span class=\"entity\" data-entity-id=\"1\">@gaurav</span>");
		Assertions.assertEquals("process-table", oc.lastMethod);
		Assertions.assertEquals(2,  oc.lastArguments.size());
		Table firstArgument = (Table) oc.lastArguments.get(0);
		List<Paragraph> expected = Arrays.asList(Paragraph.of(Arrays.asList(Word.of("thing"))), Paragraph.of(Arrays.asList(Word.of("thang"))));
		Assertions.assertEquals(expected, firstArgument.getColumnNames());
		Assertions.assertEquals(2, firstArgument.getData().size());
	}
	
	@Test
	public void testMessageResponse() throws Exception {
		execute("ban rob");
		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		Assertions.assertEquals("banWord", oc.lastMethod);
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
				Mockito.nullable(String.class), 
				Mockito.matches("abc123"),
				msg.capture(),
				Mockito.isNull(),
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull());
		Mockito.clearInvocations();
		
		String message = msg.getValue();
		System.out.println(message);
		Assertions.assertTrue(message.contains("banned words: rob"));
	}
	
	
	
	@Test
	public void testAttachmentResponse() throws Exception {
		execute("attachment");
		ArgumentCaptor<Object> att = ArgumentCaptor.forClass(Object.class);
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
			Mockito.isNull(), 
			Mockito.matches("abc123"),
			Mockito.anyString(),
			Mockito.isNull(),
			Mockito.isNull(),
			att.capture(),
			Mockito.isNull(),
			Mockito.isNull());
		Mockito.clearInvocations();
		FileDataBodyPart fdbp = (FileDataBodyPart) att.getValue();
		String contents = StreamUtils.copyToString(
			new FileInputStream((File) fdbp.getEntity()), 
			Charset.defaultCharset());
		

		Assertions.assertEquals("payload", contents);
	}
	
	@Test
	public void testFormResponse1() throws Exception {
		execute("form1");
		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
				Mockito.isNull(), 
				Mockito.matches("abc123"),
				msg.capture(),
				data.capture(),
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull());
		Mockito.clearInvocations();
		JsonNode node = new ObjectMapper().readTree(data.getValue());
		JsonNode button1 = node.get(ButtonList.KEY).get("contents").get(0);
		Assertions.assertEquals("go", button1.get("name").textValue());
	}
	
	@Test
	public void testFormResponse2() throws Exception {
		execute("form2");
		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
				Mockito.isNull(), 
				Mockito.matches("abc123"),
				msg.capture(),
				data.capture(),
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull());
		Mockito.clearInvocations();
		
		// there are no buttons for form 2.
		JsonNode node = new ObjectMapper().readTree(data.getValue());
		JsonNode buttons = node.get(ButtonList.KEY).get("contents");
		Assertions.assertEquals(0, buttons.size());	
		Assertions.assertFalse(msg.getValue().contains("<form"));
	}
	
	
	@Test
	public void testThrowsError() throws Exception {
		execute("throwsError");
		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
		
		Mockito.verify(messagesApi).v4StreamSidMessageCreatePost(
				Mockito.isNull(), 
				Mockito.matches("abc123"),
				msg.capture(),
				data.capture(),
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull(), 
				Mockito.isNull());
		
		JsonNode node = new ObjectMapper().readTree(data.getValue());
		Assertions.assertEquals("Error123", node.get(ErrorResponse.MESSAGE_KEY).asText());
		Assertions.assertTrue(msg.getValue().contains("${entity.message!'Unknown Error'}"));

		Mockito.clearInvocations();
	}
	
	@Test
	public void testOptionalPresent() throws Exception {
		execute("optionals zib zab zob <span class=\"entity\" data-entity-id=\"1\">@gaurav</span> pingu");
		Assertions.assertEquals("doList", oc.lastMethod);
		Assertions.assertEquals(3,  oc.lastArguments.size());
		
		Object firstArgument = oc.lastArguments.get(1);
		Assertions.assertEquals("gaurav", ((Optional<User>)firstArgument).get().getName());
		
		Object secondArgument = oc.lastArguments.get(0);
		Assertions.assertEquals(secondArgument, Arrays.asList(Word.of("zib"), Word.of("zab"), Word.of("zob")));
		
		Object thirdArgument = oc.lastArguments.get(2);
		Assertions.assertEquals(Word.of("pingu"), thirdArgument);
	}
	
	@Test
	public void testOptionalMissing() throws Exception {
		execute("optionals");
		Assertions.assertEquals("doList", oc.lastMethod);
		Assertions.assertEquals(3,  oc.lastArguments.size());
		Object firstArgument = oc.lastArguments.get(0);
		Assertions.assertTrue(firstArgument instanceof List);
		Assertions.assertEquals(0, ((List<?>)firstArgument).size());
		
		Object secondArgument = oc.lastArguments.get(1);
		Assertions.assertTrue(secondArgument instanceof Optional);
		Assertions.assertFalse(((Optional<?>)secondArgument).isPresent());
		
		Assertions.assertNull(oc.lastArguments.get(2));
		
	}

}