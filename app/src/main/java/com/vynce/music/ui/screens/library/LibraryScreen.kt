package com.vynce.music.ui.screens.library

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun LibraryScreen() {

    val view: LibraryViewModel = hiltViewModel()
    val state by view.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Text(
                    text = "Stream Debug Panel",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                val rawData = when (state) {
                    is StreamUiState.Loading -> "Loading stream..."
                    is StreamUiState.Error -> (state as StreamUiState.Error).message
                    is StreamUiState.Success -> (state as StreamUiState.Success).stream.toString()
                }

                SelectionContainer {
                    Text(
                        text = rawData,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Button(
                        onClick = { view.fetchStream("l3XDIjQjkzk") }
                    ) {
                        Text("Refresh")
                    }

                    Button(
                        onClick = {
                            if (state is StreamUiState.Success) {
                                val stream = (state as StreamUiState.Success).stream

                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                                clipboard.setPrimaryClip(
                                    android.content.ClipData.newPlainText(
                                        "stream_raw",
                                        stream.toString()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Copy Raw")
                    }
                }
            }
        }
    }
}