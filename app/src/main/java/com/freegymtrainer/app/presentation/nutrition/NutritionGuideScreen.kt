package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.presentation.components.AppTopBar

private class GuideSection(val title: String, val points: List<String>)

private val GUIDE = listOf(
    GuideSection(
        "How fat loss works",
        listOf(
            "Fat loss comes from eating a little less than your body uses, consistently. A gentle deficit (about 0.5 kg per week) is easier to keep up than a crash diet.",
            "Keep protein high (about 1.6-2 g per kg of body weight). It keeps you full and helps protect your muscle while the scale goes down.",
            "Fill half the plate with vegetables and salad. They are filling, high in fiber and low in calories.",
            "Strength training 2-4 times a week tells your body to keep muscle, so most of what you lose is fat.",
            "Weight changes slowly and bounces around with water and salt. Judge your progress by the 2-3 week trend, not by one day."
        )
    ),
    GuideSection(
        "Building muscle",
        listOf(
            "Eat a small surplus (about 250-300 kcal above maintenance). A big surplus mostly adds fat.",
            "Aim for 1.6-2.2 g of protein per kg spread over 3-5 meals, and train each muscle about twice a week.",
            "Carbs fuel your training. Do not cut rice, roti or oats if you want to lift hard.",
            "Sleep 7-9 hours - growth happens during recovery, not in the gym."
        )
    ),
    GuideSection(
        "The balanced plate",
        listOf(
            "Half the plate: vegetables and salad.",
            "A quarter: protein - dal, paneer, tofu, eggs, chicken, fish or soya.",
            "A quarter: smart carbs - roti, brown rice, millets, oats or potatoes.",
            "A little healthy fat: nuts, seeds, a spoon of oil or ghee. Fats are calorie-dense, so measure them."
        )
    ),
    GuideSection(
        "Protein sources (per usual serving)",
        listOf(
            "Chicken breast, 100 g cooked: about 31 g",
            "Eggs, 2 whole: about 12 g  ·  egg whites, 4: about 14 g",
            "Fish or tuna, 100 g: about 22-26 g",
            "Paneer, 100 g: about 18 g  ·  Greek yogurt, 200 g: about 20 g",
            "Soya chunks, 40 g dry: about 21 g  ·  tofu, 150 g: about 12 g",
            "Dal, 1 bowl (200 g cooked): about 18 g  ·  rajma or chole, 1 bowl: about 14-18 g",
            "Whey protein, 1 scoop (30 g): about 22-24 g"
        )
    ),
    GuideSection(
        "Eating out and Indian food, smartly",
        listOf(
            "Choose grilled or tandoori (tikka, kebabs) over fried or creamy gravies (butter, korma).",
            "Ask for dal, sabzi and roti instead of extra rice, naan or paratha.",
            "A biryani plate is usually a full meal's worth of calories - share it or skip the extras.",
            "Sweets, samosas and fried snacks are fine occasionally. Log them, enjoy them, and carry on with your next meal as planned."
        )
    ),
    GuideSection(
        "Habits that help",
        listOf(
            "Drink water through the day - a glass with each meal. Thirst is often mistaken for hunger.",
            "Eat slowly and stop when you are comfortably full.",
            "Plan the next day's meals the night before and shop with a list.",
            "Allow flexibility: aim to follow your plan about 80-90% of the time. Perfect is not required.",
            "Do not skip meals to \"make up\" for a big day. Just return to normal."
        )
    ),
    GuideSection(
        "Good to know",
        listOf(
            "The numbers in this app come from common formulas and typical food values, so treat them as a starting point and adjust after 2-3 weeks based on your results.",
            "This is general guidance, not medical advice. If you are pregnant or breastfeeding, under 18, have diabetes or another health condition, or have ever had an eating disorder, please talk to a doctor or registered dietitian first.",
            "If food or weight is causing you stress or anxiety, reaching out to a professional is a strong and sensible step."
        )
    )
)

/** Plain-language nutrition guide that goes with the Plan tab. */
@Composable
fun NutritionGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Diet guide",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            GUIDE.forEachIndexed { index, section ->
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        section.points.forEach { point ->
                            Spacer(Modifier.height(8.dp))
                            Text("•  $point", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (index == 2) { AdBanner(); Spacer(Modifier.height(12.dp)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
