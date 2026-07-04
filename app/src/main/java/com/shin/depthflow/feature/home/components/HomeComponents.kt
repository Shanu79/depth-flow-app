package com.shin.depthflow.feature.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// This file is now for smaller shared components. 
// FeatureHighlightRow and LibraryCard have their own dedicated files for better organization.

@Composable
fun SectionHeader(title: String, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        if (onActionClick != null) {
            Text(
                "View All →", 
                color = Color.Gray, 
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}







