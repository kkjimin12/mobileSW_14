package com.example.mobilequizapp

import android.R
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.google.gson.Gson


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
    var currentScreen by remember {mutableStateOf("home")}
    var currentTopic by remember {mutableStateOf("")}
    var currentQuizList by remember {mutableStateOf(listOf<Quiz>())}
    var lastScore by remember {mutableStateOf(0)}
    var lastWrongList by remember {mutableStateOf(listOf<Quiz>())}
    val context = LocalContext.current

    when(currentScreen){
        "home" -> HomeScreen(context) { topicName, fileName ->
            currentQuizList = loadQuizFromAssets(context, fileName)
            currentTopic = topicName
            currentScreen = "quiz"
        }
        "quiz" -> QuizScreen(
            topic = currentTopic,
            quizList = currentQuizList,
            onBackToHome = {currentScreen = "home"},
            onQuizFinished = {score, wrongList ->
                lastScore = score
                lastWrongList = wrongList.toList()
                currentScreen = "result"
            }
        )
        "result" -> ResultScreen(
            topic = currentTopic,
            totalQuestions = currentQuizList.size,
            wrongCount = lastWrongList.size,
            onBackToHome = { currentScreen = "home" },
            onWrongQuiz = { currentScreen = "wrong" }
        )
        "wrong" -> WrongQuizScreen()
        "ranking" -> RankingScreen()
    }
}

@Composable
fun HomeScreen(context: Context, onTopicSelected: (String, String) -> Unit){
 // 주제 선택, 틀린 문제 보기, 랭킹 보기
    Column(
        modifier = Modifier.fillMaxSize(),
    ){
        Button(onClick = {onTopicSelected("수도","capitals.json")}){
            Text("수도")
        }
        Button(onClick = {onTopicSelected("상식","general.json")}){
            Text("수도")
        }
        Button(onClick = {onTopicSelected("사자성어","idioms.json")}){
            Text("수도")
        }
        Button(onClick = {onTopicSelected("넌센스","nonsense.json")}){
            Text("수도")
        }
    }
}

//퀴즈 데이터 저장 클래스
data class Quiz(
    val question: String,
    val options: List<String>,
    val answer: Int
)
@Composable
fun QuizScreen(
    topic: String,                              //홈에서 선택한 주제
    quizList: List<Quiz>,                       //주제에 맞는 문제 리스트
    onBackToHome: () -> Unit,                   // 홈으로 돌아가기 버튼
    onQuizFinished: (Int, List<Quiz>) -> Unit   // 점수, 틀린 문제 저장
){
    var currentNum by remember { mutableStateOf(0) } //현재 문제 번호
    var selectedAnswers = remember { //선택한 답
        mutableStateListOf<Int?>().apply { repeat(quizList.size) { add(null) } }
    }
    val wrongList = remember {mutableStateListOf<Quiz>()} //틀린 문제 저장

    val currentQuiz = quizList[currentNum]

    Column ( // 상단 제목과 뒤로가기 열
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)) //배경 연한 회색
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ){
                Button(
                    onClick = onBackToHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Home", color = Color.White)
                } //홈으로가기 버튼
                Text(
                    text = topic,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f)) //홈 버튼과 균형
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        //문제 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFB3E5FC), shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ){
            Text(
                text = "${currentQuiz.question}",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            Text( //몇번째 문제인지 표시
                text = "${currentNum +1} / ${quizList.size}",
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        //선택지 4개
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentQuiz.options.forEachIndexed { index, option ->
                Button(
                    onClick = { //문제를 선택하면 바로 다음 문제로 넘어감 (정답여부 표시 X)
                        selectedAnswers[currentNum] = index
                        if(index != currentQuiz.answer){ //문제 틀리면 틀린 리스트에 저장
                            wrongList.add(currentQuiz)
                        }

                        if(currentNum < quizList.size - 1){
                            currentNum++
                        }else{ //문제 끝나면 점수 계산 후 저장
                            val score = selectedAnswers.count {it != null && it == currentQuiz.answer}
                            onQuizFinished(score, wrongList)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row (verticalAlignment = Alignment.CenterVertically){
                        Text("${index+1}. ", fontSize = 16.sp, color = Color.Black)
                        Text(option, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}
//assets json 파일 읽어오는 함수
fun loadQuizFromAssets(context: Context, fileName: String): List<Quiz>{
    val jsonString = context.assets.open(fileName)
        .bufferedReader()
        .use { it.readText() }
    return Gson().fromJson(jsonString, Array<Quiz>::class.java).toList()
}

@Composable
fun ResultScreen(
    topic: String = "",
    totalQuestions: Int = 0,
    wrongCount: Int = 0,
    onBackToHome: () -> Unit,
    onWrongQuiz: () -> Unit
){
 // 점수 표시, 홈 버튼
    val correctCount = totalQuestions - wrongCount
    val score = (correctCount.toFloat() / totalQuestions * 100).toInt()

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Box( // 트로피 그림
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 24.dp)
                .background(Color.Yellow, shape = RoundedCornerShape(50.dp))
        )
        Text(
            text = buildAnnotatedString {
                withStyle (style = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                    append(topic) // 주제 녹색
                }
                withStyle(style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                    append(" 퀴즈 성공!") // 퀴즈 성공 검은색
                }
            },
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "축하합니다!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 문제 풀이 수, 오답 수, 최종 점수 하나의 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEFEFEF), RoundedCornerShape(12.dp))
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("문제 풀이 수", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$totalQuestions", fontSize = 20.sp,fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("오답 수", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$wrongCount",  fontSize = 20.sp,fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "최종 점수: $score 점",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 버튼, 글자 수와 상관없이 크기 동일
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onBackToHome,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(text = "홈")
            }
            Button(
                onClick = onWrongQuiz,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(text = "오답보기")
            }
        }
    }
}

@Composable
fun WrongQuizScreen(){
 // 틀린 문제 목록 표시
}

@Composable
fun RankingScreen(){
 // 랭킹 표시
}