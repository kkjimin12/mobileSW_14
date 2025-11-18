package com.example.mobilequizapp

import android.content.Context
import android.os.Bundle
import android.media.MediaPlayer
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.google.gson.Gson
import kotlinx.coroutines.delay


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
            Text("상식")
        }
        Button(onClick = {onTopicSelected("사자성어","idioms.json")}){
            Text("사자성어")
        }
        Button(onClick = {onTopicSelected("넌센스","nonsense.json")}){
            Text("넌센스")
        }
    }
}

//퀴즈 데이터 저장 클래스
data class Quiz(
    val question: String,
    val options: List<String>,
    val answer: Int
)

fun playSound(context: Context, isCorrect: Boolean){
    val soundRes = if(isCorrect) R.raw.correct else R.raw.wrong
    val mp = MediaPlayer.create(context,soundRes)
    mp.setOnCompletionListener { it.release() }
    mp.start()
}

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
    var isAnswerRevealed by remember { mutableStateOf(false) }
    val wrongList = remember {mutableStateListOf<Quiz>()} //틀린 문제 저장
    val currentQuiz = quizList[currentNum]

    //효과음
    val context = LocalContext.current
//    val wrongSound = remember { MediaPlayer.create(context, R.raw.wrong) }

    //오답, 정답표시후 잠시 멈췄다 다음 문제로 넘어감
    LaunchedEffect (isAnswerRevealed){
        if(isAnswerRevealed){
            delay(500) // 0.5초 후에 넘어감
            if(currentNum < quizList.size - 1){
                currentNum ++
            }else{
                val score = selectedAnswers.count {it != null && it == quizList[it]?.answer}
                onQuizFinished(score, wrongList)
            }
            isAnswerRevealed = false
        }
    }

    Column ( // 상단 제목과 뒤로가기 열
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)) //배경 연한 회색
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ){
        Box( // 주제와 뒤로가기
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
                color = Color.Black,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        //선택지 4개 표시
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentQuiz.options.forEachIndexed { index, option ->
                val isSelected = selectedAnswers[currentNum] == index

                val backgroundColor = when{
                    isAnswerRevealed && isSelected  && index == currentQuiz.answer -> Color(0xD56CB46E) // 정답이면 초록색
                    isAnswerRevealed && isSelected  && index != currentQuiz.answer -> Color(0xDDE17D7D) // 오답이면 빨강
                    else -> Color.White
                }
                Button(
                    onClick = {
                        if(!isAnswerRevealed){
                            selectedAnswers[currentNum] = index // 선택한 답 저장
                            if(index == currentQuiz.answer){
                                playSound(context,true)
                            }
                            if(index != currentQuiz.answer) {
                                wrongList.add(currentQuiz)
                                //오답이면 소리
                                playSound(context,false)
                            } //오답 저장
                            isAnswerRevealed = true // 색상 표시
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = backgroundColor
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
            .padding(top = 150.dp, bottom = 150.dp, start = 24.dp, end = 24.dp)
            .background(Color.White, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Box( // 트로피 그림
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp)
                .background(Color.White)
        ){
            Image(
                painter = painterResource(R.drawable.prize2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
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
                .padding(horizontal = 24.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .padding(vertical = 24.dp, horizontal = 16.dp,)
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
                    text = buildAnnotatedString {
                        withStyle (style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)){
                            append("최종 점수 : ")
                        }
                        withStyle (style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)){
                            append("$score")
                        }
                        withStyle (style = SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)){
                            append(" 점")
                        }
                    },
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 버튼, 글자 수와 상관없이 크기 동일
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
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