package com.example.gemma3chatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.enableEdgeToEdge

import com.example.gemma3chatbot.ui.theme.Gemma3ChatbotTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var llmHelper: LlmService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        llmHelper = LlmService(applicationContext)
        setContent {
            Gemma3ChatbotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(llmHelper)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmHelper.close()
    }
}


@Composable
fun ChatScreen(helper: LlmService) {
    var chatLog by remember { mutableStateOf(listOf("Initializing...")) }
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val success = helper.initialize {
            chatLog = chatLog + it
        }
        if (!success) {
            chatLog = chatLog + "Model failed to initialize."
        } else {
            chatLog = chatLog + "Ready to chat!"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chatLog.forEach { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (message.startsWith("You:")) Color(0xFFD0F0C0) else Color(0xFFEEEEEE),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    fontSize = 16.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Ask something...") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    sendMessage(helper, userInput, isLoading, chatLog, coroutineScope) { newChatLog, loading ->
                        chatLog = newChatLog
                        isLoading = loading
                        userInput = ""
                    }
                })
            )
            Button(
                onClick = {
                    sendMessage(helper, userInput, isLoading, chatLog, coroutineScope) { newChatLog, loading ->
                        chatLog = newChatLog
                        isLoading = loading
                        userInput = ""
                    }
                },
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                Text("Send")
            }
        }
    }
}

private fun sendMessage(
    helper: LlmService,
    input: String,
    isLoading: Boolean,
    chatLog: List<String>,
    coroutineScope: CoroutineScope,
    onResult: (List<String>, Boolean) -> Unit
) {
    if (input.isNotBlank() && !isLoading) {
        val updatedChatLog = chatLog + "You: $input" + "Gemma: ..."
        onResult(updatedChatLog, true)
        coroutineScope.launch {
            helper.generateResponseAsync(
                input,
                onStatusUpdate = {
                    val tempLog = updatedChatLog.dropLast(1) + "Gemma: $it"
                    onResult(tempLog, true)
                },
                onResult = {
                    val finalLog = updatedChatLog.dropLast(1) + "Gemma: $it"
                    onResult(finalLog, false)
                },
                onError = {
                    val errorLog = updatedChatLog.dropLast(1) + "Error: $it"
                    onResult(errorLog, false)
                }
            )
        }
    }
}
