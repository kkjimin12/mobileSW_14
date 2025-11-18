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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    var lastWrongList by remember {mutableStateOf(listOf<Quiz>())} //전체 오답 저장변수(오답노트용)
    var currentQuizWrongList by remember { mutableStateOf(listOf<Quiz>()) } // 지금 푼 퀴즈 오답만 (결과화면용)
    val context = LocalContext.current

    when(currentScreen){
        "home" -> HomeScreen(
            context = context,
            onTopicSelected = { topicName, fileName ->
                currentQuizList = loadQuizFromAssets(context,fileName)
                currentTopic = topicName
                currentScreen = "quiz"
            },
            onWrongQuizClick = {
                currentScreen = "wrong"
            }
        )
        "quiz" -> QuizScreen(
            topic = currentTopic,
            quizList = currentQuizList,
            onBackToHome = {currentScreen = "home"},
            onQuizFinished = {score, wrongList ->
                lastScore = score
                currentQuizWrongList = wrongList.toList() // 지금 푼 퀴즈 오답 저장
                // 기존 틀린 문제와 새롭게 틀린 문제 합치기 = 전체 오답
                lastWrongList = (lastWrongList + wrongList).distinct()
                currentScreen = "result"
            }
        )
        "result" -> ResultScreen(
            topic = currentTopic,
            totalQuestions = currentQuizList.size,
            wrongCount = currentQuizWrongList.size,
            onBackToHome = { currentScreen = "home" },
            onWrongQuiz = { currentScreen = "wrong" }
        )
        "wrong" -> WrongQuizScreen(
            wrongQuizList = lastWrongList.reversed(), //최근 푼 문제를 젤 위로
            onBackToHome = {currentScreen = "home"},
            onDeleteQuiz = { quizToDelete ->
                lastWrongList = lastWrongList - quizToDelete
            }
        )
        "ranking" -> RankingScreen()
    }
}

@Composable
fun HomeScreen(
    context: Context,
    onTopicSelected: (String, String) -> Unit,
    onWrongQuizClick: () -> Unit
){
 // 주제 선택, 틀린 문제 보기, 랭킹 보기
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 50.dp),
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
        Button(onClick = onWrongQuizClick){
            Text("오답 노트 보기")
        }
    }
}

//퀴즈 데이터 저장 클래스
data class Quiz(
    val question: String,
    val options: List<String>,
    val answer: Int,
    val selectedAnswer: Int? = null, // 내가 선택한 답 저장
    val topic: String = "" // 오답 주제
)

//정답, 오답 효과음
fun playSound(context: Context, isCorrect: Boolean){
    val soundRes = if(isCorrect) R.raw.correct else R.raw.wrong
    val mp = MediaPlayer.create(context,soundRes)
    mp.setOnCompletionListener { it.release() }
    mp.start()
}

@Composable
fun QuizScreen(
    topic: String,                              // 홈에서 선택한 주제
    quizList: List<Quiz>,                       // 주제에 맞는 문제 리스트
    onBackToHome: () -> Unit,                   // 홈으로 돌아가기 버튼
    onQuizFinished: (Int, List<Quiz>) -> Unit   // 점수, 틀린 문제 저장
){
    var currentNum by remember { mutableStateOf(0) } //현재 문제 번호
    var selectedAnswers = remember { //선택한 답
        mutableStateListOf<Int?>().apply { repeat(quizList.size) { add(null) } }
    }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    val wrongList = remember {mutableStateListOf<Quiz>()} //지금 푼 퀴즈 오답 저장
    val currentQuiz = quizList[currentNum]
    val context = LocalContext.current

    //오답, 정답표시후 잠시 멈췄다 다음 문제로 넘어감
    LaunchedEffect (isAnswerRevealed){
        if(isAnswerRevealed){
            delay(1000) // 1초 후에 넘어감
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
//                .padding(12.dp)
                .height(65.dp)
        ){
            Button( //홈으로가기 버튼
                onClick = onBackToHome,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("⌂", fontSize = 20.sp, color = Color.White)
            }
            Text( //주제 텍스트
                text = topic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
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
            Text( // 문제
                text = "${currentQuiz.question}",
                fontSize = 20.sp,
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
                                // copy 호출 시 주제가 null이 아니도록 지정
                                wrongList.add(currentQuiz.copy(selectedAnswer = index, topic = currentQuiz.topic ?: "기본주제"))
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
//assets json 파일(문제목록) 읽어오는 함수
fun loadQuizFromAssets(context: Context, fileName: String): List<Quiz>{
    val jsonString = context.assets.open(fileName)
        .bufferedReader()
        .use { it.readText() }
    val quizList = Gson().fromJson(jsonString, Array<Quiz>::class.java).toList()

    return quizList.map { quiz -> // json읽을 때 주제 없으면 빈 문자열 채우기
        quiz.copy(
            question = quiz.question ?: "질문 없음",
            options = quiz.options ?: listOf("보기 없음"),
            topic = quiz.topic ?: "기본주제"
        )
    }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.weight(1f)) {
                        Text("문제 풀이 수", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$totalQuestions", fontSize = 20.sp,fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.weight(1f)) {
                        Text("정답 수", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$correctCount",  fontSize = 20.sp,fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.weight(1f)) {
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
fun WrongQuizScreen(
    wrongQuizList: List<Quiz> = listOf(),
    onBackToHome: () -> Unit,
    onDeleteQuiz: (Quiz) -> Unit
){
 // 틀린 문제 목록 표시
    Column (
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))
    ){
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(80.dp)
                .padding(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "틀린 문제 다시보기",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            Button( //홈으로 돌아가기 버튼
                onClick = onBackToHome,
                contentPadding = PaddingValues(0.dp), //버튼 내부 여백 제거
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.size(48.dp).padding(top = 20.dp, end = 20.dp)
            ) {
                Text("⌂", fontSize = 24.sp, color = Color.Black)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        //틀린 문제 목록
        if(wrongQuizList.isEmpty()){
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ){
                Text("틀린 문제가 없습니다!", fontSize = 18.sp)
            }
        }else{
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                items(wrongQuizList){ quiz->

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ){
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .background(Color(0xFFECECEC), RoundedCornerShape(4.dp))
                                        .padding(5.dp)
                                ){
                                    Text(
                                        text = quiz.topic,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = quiz.question,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                IconButton(onClick = { onDeleteQuiz(quiz) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "삭제",
                                        tint = Color.Red
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            quiz.options.forEachIndexed { i, option ->
                                val color = when{
                                    i == quiz.answer -> Color(0xD56CB46E)
                                    i == quiz.selectedAnswer -> Color(0xDDE17D7D)
                                    else -> Color.Black
                                }
                                Text(
                                    text = "${i + 1}. $option",
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingScreen(){
 // 랭킹 표시
}