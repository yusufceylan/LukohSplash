/*
 * Copyright (C) 2021 Lukoh Nam, goForer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.goforer.lukohsplash.domain.processor.photo

import android.app.DownloadManager
import android.database.Cursor
import com.goforer.base.worker.download.wrapper.DownloaderQueryWrapper
import com.goforer.lukohsplash.data.source.model.entity.photo.SaveFileInfo
import com.goforer.lukohsplash.domain.UseCase
import com.goforer.lukohsplash.presentation.vm.Params
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadPhotosUseCase
@Inject
constructor(private val downloaderQueryInterface: DownloaderQueryWrapper) : UseCase<Int>() {
    override fun run(viewModelScope: CoroutineScope, params: Params) = flow {
        var downloading = true
        val downloadManager = params.query.firstParam as DownloadManager
        val url = params.query.secondParam as String
        val file = params.query.thirdParam as File
        val fileName = url.substring(url.lastIndexOf("/") + 1).take(19)

        val query =
            downloaderQueryInterface.takeQuery(downloadManager, SaveFileInfo(url, file, fileName))

        while (downloading) {
            val cursor: Cursor = downloadManager.query(query)
            cursor.moveToFirst()
            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            ) {
                downloading = false
            }

            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            emit(status)
            cursor.close()
        }
    }.shareIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        replay = 1
    )
}
