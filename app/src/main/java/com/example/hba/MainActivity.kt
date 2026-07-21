package com.example.hba

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hba.ui.theme.HBATheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Patient(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var age: String,
    var gender: String,
    var mobile: String,
    var diagnosis: String,
    var date: String
)

object PatientStore {
    private const val FILE_NAME = "patients.json"

    fun load(context: android.content.Context): MutableList<Patient> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()
        val list = mutableListOf<Patient>()
        val arr = JSONArray(file.readText())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Patient(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    age = o.getString("age"),
                    gender = o.getString("gender"),
                    mobile = o.getString("mobile"),
                    diagnosis = o.getString("diagnosis"),
                    date = o.getString("date")
                )
            )
        }
        return list
    }

    fun save(context: android.content.Context, patients: List<Patient>) {
        val arr = JSONArray()
        patients.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("name", it.name)
            o.put("age", it.age)
            o.put("gender", it.gender)
            o.put("mobile", it.mobile)
            o.put("diagnosis", it.diagnosis)
            o.put("date", it.date)
            arr.put(o)
        }
        File(context.filesDir, FILE_NAME).writeText(arr.toString())
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HBATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PatientApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientApp() {
    val context = LocalContext.current
    var patients by remember { mutableStateOf(PatientStore.load(context)) }
    var screen by remember { mutableStateOf("list") } // list, add, detail
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isHindi by remember { mutableStateOf(true) }

    fun t(hindi: String, english: String) = if (isHindi) hindi else english

    fun persist(list: MutableList<Patient>) {
        patients = list
        PatientStore.save(context, list)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("हसन बाबू का अस्पताल") },
                actions = {
                    TextButton(onClick = { isHindi = !isHindi }) {
                        Text(if (isHindi) "EN" else "हिं", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (screen == "list") {
                FloatingActionButton(onClick = { screen = "add" }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (screen) {
                "list" -> PatientListScreen(
                    patients = patients,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onPatientClick = {
                        selectedPatient = it
                        screen = "detail"
                    },
                    t = ::t
                )
                "add" -> AddPatientScreen(
                    onSave = { newPatient ->
                        val updated = patients.toMutableList()
                        updated.add(0, newPatient)
                        persist(updated)
                        screen = "list"
                    },
                    onCancel = { screen = "list" },
                    t = ::t
                )
                "detail" -> selectedPatient?.let { p ->
                    PatientDetailScreen(
                        patient = p,
                        onBack = { screen = "list" },
                        onDelete = {
                            val updated = patients.toMutableList()
                            updated.removeAll { it.id == p.id }
                            persist(updated)
                            screen = "list"
                        },
                        t = ::t
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    patients: List<Patient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (Patient) -> Unit,
    t: (String, String) -> String
) {
    val filtered = patients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.diagnosis.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(t("नाम/बीमारी से खोजें", "Search by name/disease")) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(t("कोई मरीज़ नहीं मिला। + दबाकर जोड़ें।", "No patients yet. Tap + to add."))
            }
        } else {
            LazyColumn {
                items(filtered) { patient ->
                    PatientRow(patient, onPatientClick, t)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun PatientRow(patient: Patient, onClick: (Patient) -> Unit, t: (String, String) -> String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(patient) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 16.sp2())
            Text("${t("उम्र", "Age")}: ${patient.age}  •  ${patient.diagnosis}", fontSize = 13.sp2())
            Text("${t("दिनांक", "Date")}: ${patient.date}", fontSize = 12.sp2())
        }
        IconButton(onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.mobile}"))
            context.startActivity(intent)
        }) {
            Icon(Icons.Filled.Call, contentDescription = "Call")
        }
    }
}

fun Int.sp2() = this.toFloat().let { androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    onSave: (Patient) -> Unit,
    onCancel: () -> Unit,
    t: (String, String) -> String
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(t("नया मरीज़ जोड़ें", "Add New Patient"), fontWeight = FontWeight.Bold, fontSize = 20.sp2())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(t("नाम", "Name")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text(t("उम्र", "Age")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text(t("लिंग", "Gender")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(t("मोबाइल नंबर", "Mobile Number")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text(t("बीमारी / निदान", "Diagnosis")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(t("दिनांक (DD-MM-YYYY)", "Date (DD-MM-YYYY)")) }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))

        Row {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Patient(name = name, age = age, gender = gender, mobile = mobile, diagnosis = diagnosis, date = date))
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(t("सेव करें", "Save"))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(t("रद्द करें", "Cancel"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patient: Patient,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    t: (String, String) -> String
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 22.sp2())
        }
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(t("उम्र", "Age"), patient.age)
        DetailRow(t("लिंग", "Gender"), patient.gender)
        DetailRow(t("मोबाइल", "Mobile"), patient.mobile)
        DetailRow(t("निदान", "Diagnosis"), patient.diagnosis)
        DetailRow(t("दिनांक", "Date"), patient.date)

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.mobile}"))
                context.startActivity(intent)
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(t("कॉल करें", "Call"))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text(t("हटाएं", "Delete"))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("$label: ", fontWeight = FontWeight.Bold)
        Text(value)
    }
}