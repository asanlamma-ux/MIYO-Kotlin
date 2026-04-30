<<<<<<< HEAD
package com.miyu.reader.ui.home
=======
package com.miyu.reader.viewmodel
>>>>>>> debug

import androidx.lifecycle.ViewModel
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {
    val recentBooks: Flow<List<Book>> = bookRepository.getAllBooks()
}