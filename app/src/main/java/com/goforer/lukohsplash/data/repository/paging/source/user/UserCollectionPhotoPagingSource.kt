package com.goforer.lukohsplash.data.repository.paging.source.user

import androidx.paging.PagingState
import com.goforer.base.extension.isNullOnFlow
import com.goforer.lukohsplash.data.repository.paging.PagingErrorMessage
import com.goforer.lukohsplash.data.repository.paging.source.BasePagingSource
import com.goforer.lukohsplash.data.source.model.entity.photo.response.Photo
import com.goforer.lukohsplash.data.source.network.response.ApiEmptyResponse
import com.goforer.lukohsplash.data.source.network.response.ApiErrorResponse
import com.goforer.lukohsplash.data.source.network.response.ApiSuccessResponse
import com.goforer.lukohsplash.data.source.network.worker.NetworkBoundWorker
import com.goforer.lukohsplash.presentation.vm.Query
import kotlinx.coroutines.flow.collect
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserCollectionPhotoPagingSource
@Inject
constructor() : BasePagingSource<Int, Photo>() {
    companion object {
        internal var nextPage = 1
    }

    override fun setData(query: Query, value: MutableList<Photo>) {
        this.query = query
        pagingList = value
    }

    @SuppressWarnings("unchecked")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Photo> {
        return try {
            params.key.isNullOnFlow({
                nextPage = 1
            }, {
                nextPage = it.plus(1)
                restAPI.getUserCollectionPhotos(
                    query.firstParam as String,
                    NetworkBoundWorker.YOUR_ACCESS_KEY,
                    it.plus(1),
                    query.thirdParam as Int,
                ).collect { apiResponse ->
                    pagingList = when (apiResponse) {
                        is ApiSuccessResponse -> {
                            apiResponse.body
                        }

                        is ApiEmptyResponse -> {
                            errorMessage = PagingErrorMessage.ERROR_MESSAGE_PAGING_EMPTY
                            arrayListOf()
                        }

                        is ApiErrorResponse -> {
                            errorMessage = apiResponse.errorMessage
                            arrayListOf()
                        }
                    }
                }
            })

            if (pagingList.isNotEmpty())
                LoadResult.Page(
                    data = pagingList,
                    prevKey = null,
                    nextKey = params.key?.plus(1) ?: 1
                )
            else
                LoadResult.Error(Throwable(errorMessage))
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        } catch (exception: Exception) {
            // Handle errors in this block
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Photo>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}