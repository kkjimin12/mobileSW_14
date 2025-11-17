package com.example.mobilequizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mobilequizapp.ui.theme.MobileQuizappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizApp()
        }
    }
}

@Composable
fun QuizApp(){


}

@Composable
fun HomeScreen(){
 // 주제 선택, 틀린 문제 보기, 랭킹 보기
}

@Composable
fun QuizScreen(){
 // 문제 보여주고 답 선택
}

@Composable
fun ResultScreen(){
 // 점수 표시, 홈 버튼
}

@Composable
fun WrongQuizScreen(){
 // 틀린 문제 목록 표시
}

@Composable
fun RankingScreen(){
 // 랭킹 표시
}