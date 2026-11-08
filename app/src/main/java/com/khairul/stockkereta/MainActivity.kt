package com.khairul.stockkereta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

private fun rm(value: Double) = "RM " + NumberFormat.getNumberInstance(Locale("ms", "MY")).format(value)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { StockApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockApp(vm: StockViewModel = viewModel()) {
    val cars by vm.cars.collectAsState()
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var runner by remember { mutableStateOf("") }
    var capital by remember { mutableStateOf("") }
    var costs by remember { mutableStateOf("0") }
    var salePrice by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Car?>(null) }
    var message by remember { mutableStateOf("") }
    val active = cars.filter { it.status == "STOK" }
    val sold = cars.filter { it.status == "TERJUAL" }

    Scaffold(topBar = { TopAppBar(title = { Text("KHAIRUL STOCK") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Dashboard Stok Kereta", style = MaterialTheme.typography.headlineSmall)
                Text("Dalam stok: ${active.size}  |  Terjual: ${sold.size}")
                Text("Jumlah modal: ${rm(cars.sumOf { it.capital })}")
                Text("Jumlah kos tambahan: ${rm(cars.sumOf { it.costs })}")
                Text("Jumlah jualan: ${rm(sold.sumOf { it.salePrice })}")
                Text("Untung bersih: ${rm(sold.sumOf { it.profit })}", style = MaterialTheme.typography.titleMedium)
            }
            item { Divider() }
            item { Text("Tambah kereta masuk", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedTextField(model, { model = it }, label = { Text("Model kereta") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(year, { year = it }, label = { Text("Tahun") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(plate, { plate = it }, label = { Text("Nombor pendaftaran") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(runner, { runner = it }, label = { Text("Runner") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(capital, { capital = it }, label = { Text("Modal beli (RM)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(costs, { costs = it }, label = { Text("Kos tambahan (RM)") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(onClick = {
                    val c = capital.toDoubleOrNull()
                    val k = costs.toDoubleOrNull()
                    if (model.isBlank() || c == null || k == null) message = "Isi model, modal dan kos dengan betul"
                    else {
                        vm.addCar(Car(model = model.trim(), year = year.trim(), plate = plate.trim(), runner = runner.trim(), capital = c, costs = k)) {
                            message = it ?: "Rekod berjaya disimpan"
                            if (it == null) { model=""; year=""; plate=""; runner=""; capital=""; costs="0" }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Simpan stok masuk") }
                if (message.isNotBlank()) Text(message)
            }
            item { Divider() }
            item { Text("Senarai kenderaan", style = MaterialTheme.typography.titleLarge) }
            if (cars.isEmpty()) item { Text("Belum ada rekod. Masukkan kereta pertama di atas.") }
            items(cars, key = { it.id }) { car ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${car.model} ${car.year}", style = MaterialTheme.typography.titleMedium)
                        Text("No. ${car.plate} | Runner: ${car.runner}")
                        Text("Modal: ${rm(car.capital)} | Kos: ${rm(car.costs)}")
                        Text("Status: ${car.status}")
                        if (car.status == "TERJUAL") {
                            Text("Harga jual: ${rm(car.salePrice)} | Untung: ${rm(car.profit)}")
                        } else {
                            OutlinedButton(onClick = { selected = car }) { Text("Tandakan terjual") }
                        }
                        TextButton(onClick = { vm.deleteCar(car.id) }) { Text("Padam rekod") }
                    }
                }
            }
        }
    }
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Rekod jualan - ${selected!!.model}") },
            text = {
                OutlinedTextField(salePrice, { salePrice = it }, label = { Text("Harga jual (RM)") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val price = salePrice.toDoubleOrNull()
                    if (price != null) vm.sellCar(selected!!.id, price) { message = it ?: "Jualan direkodkan"; selected = null; salePrice = "" }
                    else message = "Masukkan harga jual yang sah"
                }) { Text("Simpan jualan") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Batal") } }
        )
    }
}
