package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.rememberAssetImageBitmap

private enum class DietFilter { ALL, VEG, NON_VEG }

@Composable
fun FoodListScreen(container: AppContainer, onBack: () -> Unit, onOpenFood: (Long) -> Unit) {
    val viewModel: FoodListViewModel = viewModel(factory = GenericViewModelFactory { FoodListViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var filter by remember { mutableStateOf(DietFilter.ALL) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Database") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        val filtered = remember(state.foods, filter, query) {
            state.foods
                .filter { food ->
                    when (filter) {
                        DietFilter.ALL -> true
                        DietFilter.VEG -> food.isVeg()
                        DietFilter.NON_VEG -> !food.isVeg()
                    }
                }
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            FoodDatabaseHero(itemCount = state.foods.size)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search foods...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == DietFilter.ALL, onClick = { filter = DietFilter.ALL }, label = { Text("All") })
                FilterChip(
                    selected = filter == DietFilter.VEG,
                    onClick = { filter = DietFilter.VEG },
                    label = { Text("Veg") },
                    leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = filter == DietFilter.NON_VEG,
                    onClick = { filter = DietFilter.NON_VEG },
                    label = { Text("Non-Veg") },
                    leadingIcon = { Icon(Icons.Filled.Egg, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${filtered.size} items  •  values shown are per the default serving size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyState("No foods match your filters.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { food ->
                        FoodRow(food, onClick = { onOpenFood(food.id) })
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FoodDatabaseHero(itemCount: Int) {
    val photo = rememberAssetImageBitmap("nutrients_pics/nutrients.jpg")

    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)))
                )
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                )
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("Food Database", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                "$itemCount foods - tap any item for vitamins, minerals & full nutrition facts",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (food.isVeg()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (food.isVeg()) Icons.Filled.Eco else Icons.Filled.Egg,
                    contentDescription = if (food.isVeg()) "Vegetarian" else "Non-vegetarian",
                    tint = if (food.isVeg()) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                Text(food.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val factor = food.defaultServingGrams / 100f
                Text(
                    "${(food.caloriesPer100g * factor).toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${food.defaultServingGrams.toInt()}g  •  ${(food.proteinPer100g * factor).toInt()}g protein",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
