package com.example.sushibooking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.sushibooking.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = BookingDatabase.getDatabase(applicationContext)
        val vm = BookingViewModel(db.bookingDao())
        setContent { SushiBookingApp(vm) }
    }
}

class BookingViewModel(private val dao: BookingDao) : ViewModel() {
    val bookings = dao.observeBookings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _apiStatus = MutableStateFlow("Menu data has not been loaded yet.")
    val apiStatus: StateFlow<String> = _apiStatus.asStateFlow()

    private val _chat = MutableStateFlow(
        listOf("Sushi Helper" to "Hi! Ask me about booking, opening hours, or choosing a branch.")
    )
    val chat: StateFlow<List<Pair<String, String>>> = _chat.asStateFlow()

    fun loadSushiRecipes() {
        viewModelScope.launch {
            _apiStatus.value = "Loading recipes from external API..."
            try {
                var response = RecipeApi.instance.searchRecipes("Japanese")
                if (response.recipes.isEmpty()) {
                    response = RecipeApi.instance.searchRecipes("Asian")
                }
                if (response.recipes.isEmpty()) {
                    response = RecipeApi.instance.getAllRecipes()
                }
                _recipes.value = response.recipes
                _apiStatus.value = "Loaded ${response.recipes.size} recipe results from DummyJSON."
            } catch (e: Exception) {
                _apiStatus.value = "Could not load API data: ${e.localizedMessage ?: "Check internet connection."}"
            }
        }
    }

    fun addBooking(branch: String, date: String, time: String, guests: Int) {
        viewModelScope.launch {
            dao.insert(Booking(branch = branch, date = date, time = time, guests = guests))
        }
    }

    fun cancelBooking(id: Int) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun sendChat(message: String) {
        val question = message.trim()
        if (question.isEmpty()) return
        val answer = when {
            "book" in question.lowercase() || "reservation" in question.lowercase() ->
                "To book a table, open Make a Booking, choose a branch, date, time, and number of guests, then confirm."
            "hour" in question.lowercase() || "open" in question.lowercase() ->
                "Opening hours in this prototype are examples only. Please check with the restaurant for actual hours."
            "branch" in question.lowercase() || "location" in question.lowercase() ->
                "Choose a branch on the booking screen. This demo uses sample branch names, not a live restaurant directory."
            "cancel" in question.lowercase() ->
                "Open My Bookings and tap Cancel on the reservation you want to remove."
            "sushi" in question.lowercase() || "menu" in question.lowercase() ->
                "Open the Home screen and load the sample sushi-related recipe results from the external API."
            else ->
                "I’m Sushi Helper, a simple demo chatbot. Try asking how to book, where to find branches, or how to cancel."
        }
        _chat.value = _chat.value + (question to answer)
    }
}

private enum class AppScreen(val label: String) {
    HOME("Home"), BOOK("Book"), BOOKINGS("My Bookings"), CHAT("Sushi Helper")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SushiBookingApp(vm: BookingViewModel) {
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var message by remember { mutableStateOf("") }
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val apiStatus by vm.apiStatus.collectAsStateWithLifecycle()
    val chat by vm.chat.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFC62828),
            secondary = androidx.compose.ui.graphics.Color(0xFF795548)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sushi Table") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = screen == AppScreen.HOME,
                        onClick = { screen = AppScreen.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = screen == AppScreen.BOOK,
                        onClick = { screen = AppScreen.BOOK },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        label = { Text("Book") }
                    )
                    NavigationBarItem(
                        selected = screen == AppScreen.BOOKINGS,
                        onClick = { screen = AppScreen.BOOKINGS },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                        label = { Text("Bookings") }
                    )
                    NavigationBarItem(
                        selected = screen == AppScreen.CHAT,
                        onClick = { screen = AppScreen.CHAT },
                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        label = { Text("AI Help") }
                    )
                }
            }
        ) { padding ->
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    apiStatus = apiStatus,
                    recipes = recipes,
                    onLoadRecipes = vm::loadSushiRecipes,
                    onBook = { screen = AppScreen.BOOK }
                )
                AppScreen.BOOK -> BookingScreen(
                    modifier = Modifier.padding(padding),
                    onConfirm = { branch, date, time, guests ->
                        vm.addBooking(branch, date, time, guests)
                        screen = AppScreen.BOOKINGS
                    }
                )
                AppScreen.BOOKINGS -> BookingsScreen(
                    modifier = Modifier.padding(padding),
                    bookings = bookings,
                    onCancel = vm::cancelBooking
                )
                AppScreen.CHAT -> ChatScreen(
                    modifier = Modifier.padding(padding),
                    messages = chat,
                    message = message,
                    onMessageChange = { message = it },
                    onSend = {
                        vm.sendChat(message)
                        message = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    apiStatus: String,
    recipes: List<Recipe>,
    onLoadRecipes: () -> Unit,
    onBook: () -> Unit
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Welcome to Sushi Table 🍣", style = MaterialTheme.typography.headlineSmall)
        Text("Reserve your seat for your next sushi meal.")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Make a reservation", style = MaterialTheme.typography.titleMedium)
                Text("Choose a branch, date, time, and number of guests.")
                Button(onClick = onBook, modifier = Modifier.fillMaxWidth()) {
                    Text("Book a Table")
                }
            }
        }
        Text("External API Demo", style = MaterialTheme.typography.titleLarge)
        Text("Press the button to fetch sushi-related recipe data from DummyJSON.")
        Button(onClick = onLoadRecipes, modifier = Modifier.fillMaxWidth()) {
            Text("Load Data from API")
        }
        Text(apiStatus, style = MaterialTheme.typography.bodyMedium)
        recipes.take(8).forEach { recipe ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                    Text("Cuisine: ${recipe.cuisine ?: "Not provided"}")
                    Text("Difficulty: ${recipe.difficulty ?: "Not provided"}")
                }
            }
        }
        Text(
            "Note: recipe data is used only to demonstrate API connectivity. It is not a live restaurant menu.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingScreen(
    modifier: Modifier,
    onConfirm: (String, String, String, Int) -> Unit
) {
    var branch by remember { mutableStateOf("Siam Branch") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var time by remember { mutableStateOf("18:00") }
    var guests by remember { mutableIntStateOf(2) }
    var dateError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val branches = listOf("Siam Branch", "CentralWorld Branch", "Sukhumvit Branch")
    val times = listOf("11:00", "12:00", "13:00", "17:00", "18:00", "19:00", "20:00")

    if (showDatePicker) {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val initialMillis = runCatching {
            utcFormat.parse(date)?.time
        }.getOrNull() ?: System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = utcFormat.format(Date(millis))
                            dateError = false
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Make a Booking", style = MaterialTheme.typography.headlineSmall)
        Text("All branch names and time slots in this demo are sample data.")
        var branchExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = branchExpanded,
            onExpandedChange = { branchExpanded = !branchExpanded }
        ) {
            OutlinedTextField(
                value = branch,
                onValueChange = {},
                readOnly = true,
                label = { Text("Restaurant branch") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = branchExpanded,
                onDismissRequest = { branchExpanded = false }
            ) {
                branches.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = { branch = item; branchExpanded = false }
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date (YYYY-MM-DD)") },
                supportingText = { Text("Tap to select date from calendar") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    }
                },
                isError = dateError,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }
        if (dateError) Text("Enter a date in YYYY-MM-DD format.")
        Text("Choose a time", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            times.chunked(3).forEach { rowTimes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTimes.forEach { slot ->
                        FilterChip(
                            selected = time == slot,
                            onClick = { time = slot },
                            label = { Text(slot) }
                        )
                    }
                }
            }
        }
        Text("Number of guests", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { if (guests > 1) guests-- }) { Text("−") }
            Text("$guests guests", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { if (guests < 12) guests++ }) { Text("+") }
        }
        Button(
            onClick = {
                val valid = runCatching {
                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        isLenient = false
                    }.parse(date)
                    parsed != null && date.length == 10
                }.getOrDefault(false)
                if (valid) onConfirm(branch, date, time, guests) else dateError = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirm Booking") }
        Text(
            "Demo only: this saves the reservation on this device. It does not contact a real restaurant.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BookingsScreen(
    modifier: Modifier,
    bookings: List<Booking>,
    onCancel: (Int) -> Unit
) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("My Bookings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (bookings.isEmpty()) {
            Text("You have no bookings yet. Go to Book to make a reservation.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(booking.branch, style = MaterialTheme.typography.titleMedium)
                            Text("Date: ${booking.date}")
                            Text("Time: ${booking.time}")
                            Text("Guests: ${booking.guests}")
                            Text("Booking ID: ${booking.id}")
                            OutlinedButton(onClick = { onCancel(booking.id) }) {
                                Text("Cancel Booking")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    modifier: Modifier,
    messages: List<Pair<String, String>>,
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Sushi Helper", style = MaterialTheme.typography.headlineSmall)
        Text("A simple demo chatbot with predefined answers.")
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.size) { index ->
                val (question, answer) = messages[index]
                if (index == 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(answer, Modifier.padding(12.dp))
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("You: $question", style = MaterialTheme.typography.labelLarge)
                            Text("Sushi Helper: $answer")
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                label = { Text("Ask a question") },
                singleLine = true
            )
            Button(onClick = onSend) { Text("Send") }
        }
    }
}
