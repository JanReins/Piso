package com.janreins.piso.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.janreins.piso.data.models.UserCategory
import com.janreins.piso.data.models.UserSubcategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // --- Categories ---
    @Query("SELECT * FROM user_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<UserCategory>>

    @Query("SELECT * FROM user_categories WHERE kind = :kind ORDER BY id ASC")
    fun getCategoriesByKind(kind: String): Flow<List<UserCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: UserCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<UserCategory>)

    @Update
    suspend fun updateCategory(category: UserCategory)

    @Delete
    suspend fun deleteCategory(category: UserCategory)

    @Query("DELETE FROM user_categories")
    suspend fun clearCategories()

    // --- Subcategories ---
    @Query("SELECT * FROM user_subcategories ORDER BY id ASC")
    fun getAllSubcategories(): Flow<List<UserSubcategory>>

    @Query("SELECT * FROM user_subcategories WHERE parentCategoryName = :parentName ORDER BY id ASC")
    fun getSubcategoriesForParent(parentName: String): Flow<List<UserSubcategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: UserSubcategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategories(subcategories: List<UserSubcategory>)

    @Update
    suspend fun updateSubcategory(subcategory: UserSubcategory)

    @Delete
    suspend fun deleteSubcategory(subcategory: UserSubcategory)

    @Query("DELETE FROM user_subcategories")
    suspend fun clearSubcategories()

    // --- Cascade Name Updates ---
    @Query("UPDATE user_subcategories SET parentCategoryName = :newName WHERE parentCategoryName = :oldName")
    suspend fun updateSubcategoriesParentName(oldName: String, newName: String)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateTransactionsCategoryName(oldName: String, newName: String)

    @Query("UPDATE transactions SET subcategory = :newSubName WHERE category = :parentName AND subcategory = :oldSubName")
    suspend fun updateTransactionsSubcategoryName(parentName: String, oldSubName: String, newSubName: String)

    @Query("UPDATE budgets SET category = :newName WHERE category = :oldName")
    suspend fun updateBudgetsCategoryName(oldName: String, newName: String)

    // --- Transaction Count Checks ---
    @Query("SELECT COUNT(*) FROM transactions WHERE category = :categoryName")
    suspend fun countTransactionsForCategory(categoryName: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE category = :parentName AND subcategory = :subcategoryName")
    suspend fun countTransactionsForSubcategory(parentName: String, subcategoryName: String): Int
}
