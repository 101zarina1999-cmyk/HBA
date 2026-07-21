package com.example.hba

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hba.ui.theme.HBATheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ---------- DATA MODELS ----------

data class Medicine(var name: String, var dosage: String, var duration: String)
data class LabTest(var testName: String, var date: String, var result: String)
data class Billing(
    var consultationFee: String = "0",
    var labCharges: String = "0",
    var pharmacyCharges: String = "0",
    var paymentMode: String = "Cash"
) {
    fun total(): Int {
        return (consultationFee.toIntOrNull() ?: 0) +
                (labCharges.toIntOrNull() ?: 0) +
                (pharmacyCharges.toIntOrNull() ?: 0)
    }
}

data class Visit(
    val visitId: Long = System.currentTimeMillis(),
    var date: String,
    var doctor: String,
    var symptoms: String,
    var diagnosis: String,
    var medicines: MutableList<Medicine> = mutableListOf(),
    var followUpDate: String = "",
    var labTests: MutableList<LabTest> = mutableListOf(),
    var billing: Billing = Billing()
)

data class Patient(
    val id: String,
    var name: String,
    var age: String,
    var gender: String,
    var address: String,
    var contact: String,
    var aadhaar: String = "",
    var pastIllness: String = "",
    var allergies: String = "",
    var familyHistory: String = "",
    var isEmergency: Boolean = false,
    var visits: MutableList<Visit> = mutableListOf()
)

// ---------- STORAGE ----------

object DataStore {
    private const val FILE_NAME = "patients_full.json"
    private const val PREFS = "hba_prefs"

    fun load(context: Context): MutableList<Patient> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()
        val list = mutableListOf<Patient>()
        val arr = JSONArray(file.readText())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val visits = mutableListOf<Visit>()
            val visitArr = o.optJSONArray("visits") ?: JSONArray()
            for (j in 0 until visitArr.length()) {
                val v = visitArr.getJSONObject(j)
                val meds = mutableListOf<Medicine>()
                val medArr = v.optJSONArray("medicines") ?: JSONArray()
                for (k in 0 until medArr.length()) {
                    val m = medArr.getJSONObject(k)
                    meds.add(Medicine(m.getString("name"), m.getString("dosage"), m.getString("duration")))
                }
                val labs = mutableListOf<LabTest>()
                val labArr = v.optJSONArray("labTests") ?: JSONArray()
                for (k in 0 until labArr.length()) {
                    val l = labArr.getJSONObject(k)
                    labs.add(LabTest(l.getString("testName"), l.getString("date"), l.getString("result")))
                }
                val b = v.optJSONObject("billing")
                val billing = Billing(
                    consultationFee = b?.optString("consultationFee", "0") ?: "0",
                    labCharges = b?.optString("labCharges", "0") ?: "0",
                    pharmacyCharges = b?.optString("pharmacyCharges", "0") ?: "0",
                    paymentMode = b?.optString("paymentMode", "Cash") ?: "Cash"
                )
                visits.add(
                    Visit(
                        visitId = v.getLong("visitId"),
                        date = v.getString("date"),
                        doctor = v.getString("doctor"),
                        symptoms = v.getString("symptoms"),
                        diagnosis = v.getString("diagnosis"),
                        medicines = meds,
                        followUpDate = v.optString("followUpDate", ""),
                        labTests = labs,
                        billing = billing
                    )
                )
            }
            list.add(
                Patient(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    age = o.getString("age"),
                    gender = o.getString("gender"),
                    address = o.getString("address"),
                    contact = o.getString("contact"),
                    aadhaar = o.optString("aadhaar", ""),
                    pastIllness = o.optString("pastIllness", ""),
                    allergies = o.optString("allergies", ""),
                    familyHistory = o.optString("familyHistory", ""),
                    isEmergency = o.optBoolean("isEmergency", false),
                    visits = visits
                )
            )
        }
        return list
    }

    fun save(context: Context, patients: List<Patient>) {
        val arr = JSONArray()
        patients.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("age", p.age)
            o.put("gender", p.gender)
            o.put("address", p.address)
            o.put("contact", p.contact)
            o.put("aadhaar", p.aadhaar)
            o.put("pastIllness", p.pastIllness)
            o.put("allergies", p.allergies)
            o.put("familyHistory", p.familyHistory)
            o.put("isEmergency", p.isEmergency)
            val visitArr = JSONArray()
            p.visits.forEach { v ->
                val vo = JSONObject()
                vo.put("visitId", v.visitId)
                vo.put("date", v.date)
                vo.put("doctor", v.doctor)
                vo.put("symptoms", v.symptoms)
                vo.put("diagnosis", v.diagnosis)
                vo.put("followUpDate", v.followUpDate)
                val medArr = JSONArray()
                v.medicines.forEach { m ->
                    val mo = JSONObject()
                    mo.put("name", m.name); mo.put("dosage", m.dosage); mo.put("duration", m.duration)
                    medArr.put(mo)
                }
                vo.put("medicines", medArr)
                val labArr = JSONArray()
                v.labTests.forEach { l ->
                    val lo = JSONObject()
                    lo.put("testName", l.testName); lo.put("date", l.date); lo.put("result", l.result)
                    labArr.put(lo)
                }
                vo.put("labTests", labArr)
                val bo = JSONObject()
                bo.put("consultationFee", v.billing.consultationFee)
                bo.put("labCharges", v.billing.labCharges)
                bo.put("pharmacyCharges", v.billing.pharmacyCharges)
                bo.put("paymentMode", v.billing.paymentMode)
                vo.put("billing", bo)
                visitArr.put(vo)
            }
            o.put("visits", visitArr)
            arr.put(o)
        }
        File(context.filesDir, FILE_NAME).writeText(arr.toString())
    }

    fun nextPatientId(context: Context, patients: List<Patient>): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var counter = prefs.getInt("patient_counter", patients.size)
        counter += 1
        prefs.edit().putInt("patient_counter", counter).apply()
        return "HBA-%04d".format(counter)
    }

    fun getPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("app_password", "1234") ?: "1234"
    }

    fun setPassword(context: Context, pass: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("app_password", pass).apply()
    }

    fun exportBackup(context: Context, patients: List<Patient>): Boolean {
        return try {
            val arr = JSONArray()
            patients.forEach { p ->
                val o = JSONObject()
                o.put("id", p.id); o.put("name", p.name); o.put("age", p.age)
                o.put("gender", p.gender); o.put("contact", p.contact)
                arr.put(o)
            }
            val fileName = "HBA_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/HBA_Backups")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(arr.toString().toByteArray())
                }
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

// ---------- MAIN ACTIVITY ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HBATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf(false) }
    var patients by remember { mutableStateOf(DataStore.load(context)) }
    var screen by remember { mutableStateOf("home") }
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }

    fun persist(list: MutableList<Patient>) {
        patients = list
        DataStore.save(context, list)
    }

    if (!isLoggedIn) {
        LoginScreen(onSuccess = { isLoggedIn = true })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == "home" || screen == "list" || screen == "register" || screen == "detail" || screen == "visit",
                    onClick = { screen = "list" },
                    icon = { Icon(Icons.Filled.List, null) },
                    label = { Text("मरीज़") }
                )
                NavigationBarItem(
                    selected = screen == "emergency",
                    onClick = { screen = "emergency" },
                    icon = { Icon(Icons.Filled.Warning, null) },
                    label = { Text("आपातकाल") }
                )
                NavigationBarItem(
                    selected = screen == "analytics",
                    onClick = { screen = "analytics" },
                    icon = { Icon(Icons.Filled.Info, null) },
                    label = { Text("रिपोर्ट") }
                )
                NavigationBarItem(
                    selected = screen == "backup",
                    onClick = { screen = "backup" },
                    icon = { Icon(Icons.Filled.Lock, null) },
                    label = { Text("बैकअप") }
                )
            }
        },
        floatingActionButton = {
            if (screen == "list") {
                FloatingActionButton(onClick = { screen = "register" }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                "list" -> PatientListScreen(
                    patients = patients,
                    onPatientClick = { selectedPatient = it; screen = "detail" }
                )
                "register" -> RegisterPatientScreen(
                    onSave = { p ->
                        val updated = patients.toMutableList()
                        updated.add(0, p)
                        persist(updated)
                        screen = "list"
                    },
                    onCancel = { screen = "list" },
                    context = context,
                    patients = patients,
                    isEmergency = false
                )
                "emergency" -> RegisterPatientScreen(
                    onSave = { p ->
                        val updated = patients.toMutableList()
                        updated.add(0, p)
                        persist(updated)
                        screen = "list"
                    },
                    onCancel = { screen = "home" },
                    context = context,
                    patients = patients,
                    isEmergency = true
                )
                "detail" -> selectedPatient?.let { p ->
                    PatientDetailScreen(
                        patient = p,
                        onBack = { screen = "list" },
                        onAddVisit = { screen = "visit" },
                        onDelete = {
                            val updated = patients.toMutableList()
                            updated.removeAll { it.id == p.id }
                            persist(updated)
                            screen = "list"
                        }
                    )
                }
                "visit" -> selectedPatient?.let { p ->
                    AddVisitScreen(
                        onSave = { visit ->
                            val updated = patients.toMutableList()
                            val idx = updated.indexOfFirst { it.id == p.id }
                            if (idx >= 0) {
                                updated[idx].visits.add(0, visit)
                                persist(updated)
                                selectedPatient = updated[idx]
                            }
                            screen = "detail"
                        },
                        onCancel = { screen = "detail" }
                    )
                }
                "analytics" -> AnalyticsScreen(patients)
                "backup" -> BackupScreen(patients)
            }
        }
    }
}

// ---------- LOGIN ----------

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("हसन बाबू का अस्पताल", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("Patient Record System", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("पासवर्ड (Default: 1234)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (password == DataStore.getPassword(context)) {
                    onSuccess()
                } else {
                    error = "गलत पासवर्ड"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("लॉगिन करें")
        }
    }
}

// ---------- PATIENT LIST ----------

@Composable
fun PatientListScreen(patients: List<Patient>, onPatientClick: (Patient) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = patients.filter {
        it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
    }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("नाम या ID से खोजें") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("कोई मरीज़ नहीं। नीचे + दबाकर जोड़ें।")
            }
        } else {
            LazyColumn {
                items(filtered) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPatientClick(p) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (p.isEmergency) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🚨", fontSize = 14.sp)
                                }
                            }
                            Text("${p.id} • ${p.age} yrs • ${p.gender}", fontSize = 13.sp)
                            Text(p.contact, fontSize = 12.sp)
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

// ---------- REGISTER PATIENT ----------

@Composable
fun RegisterPatientScreen(
    onSave: (Patient) -> Unit,
    onCancel: () -> Unit,
    context: Context,
    patients: List<Patient>,
    isEmergency: Boolean
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var pastIllness by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var familyHistory by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (isEmergency) "🚨 आपातकालीन एंट्री" else "नया मरीज़ पंजीकरण",
            fontWeight = FontWeight.Bold, fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("नाम *") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("उम्र") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("लिंग") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("मोबाइल नंबर *") }, modifier = Modifier.fillMaxWidth())

        if (!isEmergency) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("पता") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, label = { Text("आधार/ID (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = pastIllness, onValueChange = { pastIllness = it }, label = { Text("पुरानी बीमारी/ऑपरेशन") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("एलर्जी") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = familyHistory, onValueChange = { familyHistory = it }, label = { Text("पारिवारिक इतिहास (BP, शुगर आदि)") }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Button(
                onClick = {
                    if (name.isNotBlank() && contact.isNotBlank()) {
                        val id = DataStore.nextPatientId(context, patients)
                        onSave(
                            Patient(
                                id = id, name = name, age = age, gender = gender,
                                address = address, contact = contact, aadhaar = aadhaar,
                                pastIllness = pastIllness, allergies = allergies,
                                familyHistory = familyHistory, isEmergency = isEmergency
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("सेव करें") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("रद्द करें") }
        }
    }
}

// ---------- PATIENT DETAIL ----------

@Composable
fun PatientDetailScreen(patient: Patient, onBack: () -> Unit, onAddVisit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Column {
                Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(patient.id, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("उम्र: ${patient.age}   लिंग: ${patient.gender}")
        Text("पता: ${patient.address}")
        Text("मोबाइल: ${patient.contact}")
        if (patient.pastIllness.isNotBlank()) Text("पुरानी बीमारी: ${patient.pastIllness}")
        if (patient.allergies.isNotBlank()) Text("एलर्जी: ${patient.allergies}")
        if (patient.familyHistory.isNotBlank()) Text("पारिवारिक इतिहास: ${patient.familyHistory}")

        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${patient.contact}"))
                context.startActivity(intent)
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Call, null); Spacer(modifier = Modifier.width(4.dp)); Text("कॉल")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddVisit, modifier = Modifier.weight(1f)) { Text("+ नई विज़िट") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("मरीज़ हटाएं") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("पिछली विज़िट (${patient.visits.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        patient.visits.forEach { v ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text("दिनांक: ${v.date}   डॉक्टर: ${v.doctor}", fontWeight = FontWeight.Bold)
                Text("लक्षण: ${v.symptoms}")
                Text("निदान: ${v.diagnosis}")
                if (v.medicines.isNotEmpty()) {
                    Text("दवाइयाँ:")
                    v.medicines.forEach { m -> Text("  • ${m.name} — ${m.dosage} — ${m.duration}") }
                }
                if (v.labTests.isNotEmpty()) {
                    Text("जांच:")
                    v.labTests.forEach { l -> Text("  • ${l.testName} (${l.date}): ${l.result}") }
                }
                Text("बिल: ₹${v.billing.total()} (${v.billing.paymentMode})")
                if (v.followUpDate.isNotBlank()) Text("अगली विज़िट: ${v.followUpDate}", color = MaterialTheme.colorScheme.error)
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// ---------- ADD VISIT ----------

@Composable
fun AddVisitScreen(onSave: (Visit) -> Unit, onCancel: () -> Unit) {
    val today = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var doctor by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var followUpDate by remember { mutableStateOf("") }

    val medicines = remember { mutableStateListOf<Medicine>() }
    var medName by remember { mutableStateOf("") }
    var medDosage by remember { mutableStateOf("") }
    var medDuration by remember { mutableStateOf("") }

    val labTests = remember { mutableStateListOf<LabTest>() }
    var labName by remember { mutableStateOf("") }
    var labResult by remember { mutableStateOf("") }

    var consultationFee by remember { mutableStateOf("0") }
    var labCharges by remember { mutableStateOf("0") }
    var pharmacyCharges by remember { mutableStateOf("0") }
    var paymentMode by remember { mutableStateOf("Cash") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("नई OPD विज़िट", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("दिनांक") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = doctor, onValueChange = { doctor = it }, label = { Text("डॉक्टर") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = symptoms, onValueChange = { symptoms = it }, label = { Text("लक्षण") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("निदान") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = followUpDate, onValueChange = { followUpDate = it }, label = { Text("अगली विज़िट (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))
        Text("प्रिस्क्रिप्शन", fontWeight = FontWeight.Bold)
        medicines.forEach { m -> Text("• ${m.name} — ${m.dosage} — ${m.duration}") }
        Row {
            OutlinedTextField(value = medName, onValueChange = { medName = it }, label = { Text("दवा") }, modifier = Modifier.weight(1f))
        }
        Row {
            OutlinedTextField(value = medDosage, onValueChange = { medDosage = it }, label = { Text("खुराक") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(value = medDuration, onValueChange = { medDuration = it }, label = { Text("अवधि") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            if (medName.isNotBlank()) {
                medicines.add(Medicine(medName, medDosage, medDuration))
                medName = ""; medDosage = ""; medDuration = ""
            }
        }) { Text("+ दवा जोड़ें") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("जांच / लैब टेस्ट", fontWeight = FontWeight.Bold)
        labTests.forEach { l -> Text("• ${l.testName}: ${l.result}") }
        Row {
            OutlinedTextField(value = labName, onValueChange = { labName = it }, label = { Text("जांच का नाम") }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(value = labResult, onValueChange = { labResult = it }, label = { Text("परिणाम") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            if (labName.isNotBlank()) {
                labTests.add(LabTest(labName, date, labResult))
                labName = ""; labResult = ""
            }
        }) { Text("+ जांच जोड़ें") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("बिलिंग", fontWeight = FontWeight.Bold)
        OutlinedTextField(value = consultationFee, onValueChange = { consultationFee = it }, label = { Text("परामर्श शुल्क") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = labCharges, onValueChange = { labCharges = it }, label = { Text("जांच शुल्क") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(value = pharmacyCharges, onValueChange = { pharmacyCharges = it }, label = { Text("दवा शुल्क") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            listOf("Cash", "UPI").forEach { mode ->
                FilterChip(
                    selected = paymentMode == mode,
                    onClick = { paymentMode = mode },
                    label = { Text(mode) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Button(
                onClick = {
                    onSave(
                        Visit(
                            date = date, doctor = doctor, symptoms = symptoms, diagnosis = diagnosis,
                            medicines = medicines.toMutableList(), followUpDate = followUpDate,
                            labTests = labTests.toMutableList(),
                            billing = Billing(consultationFee, labCharges, pharmacyCharges, paymentMode)
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("विज़िट सेव करें") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("रद्द करें") }
        }
    }
}

// ---------- ANALYTICS ----------

@Composable
fun AnalyticsScreen(patients: List<Patient>) {
    val today = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()) }
    val allVisits = patients.flatMap { it.visits }
    val todayCount = allVisits.count { it.date == today }
    val diseaseCount = allVisits.groupingBy { it.diagnosis }.eachCount().entries.sortedByDescending { it.value }.take(5)
    val medicineCount = allVisits.flatMap { it.medicines }.groupingBy { it.name }.eachCount().entries.sortedByDescending { it.value }.take(5)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("रिपोर्ट / एनालिटिक्स", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("कुल मरीज़: ${patients.size}", fontSize = 16.sp)
        Text("आज की OPD संख्या: $todayCount", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("सबसे आम बीमारियाँ:", fontWeight = FontWeight.Bold)
        if (diseaseCount.isEmpty()) Text("कोई डेटा नहीं")
        diseaseCount.forEach { Text("• ${it.key}: ${it.value} बार") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("सबसे ज़्यादा दी गई दवाइयाँ:", fontWeight = FontWeight.Bold)
        if (medicineCount.isEmpty()) Text("कोई डेटा नहीं")
        medicineCount.forEach { Text("• ${it.key}: ${it.value} बार") }
    }
}

// ---------- BACKUP ----------

@Composable
fun BackupScreen(patients: List<Patient>) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("डेटा बैकअप", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("कुल मरीज़: ${patients.size}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val ok = DataStore.exportBackup(context, patients)
            message = if (ok) "बैकअप सफल! Downloads/HBA_Backups में सेव हुआ।" else "बैकअप विफल"
        }) { Text("अभी बैकअप करें") }
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(message)
        }
    }
}