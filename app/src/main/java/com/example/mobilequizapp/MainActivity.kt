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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val context = LocalContext.current

    RankingScreen {

    }
}
//랭킹 위한 게임 기록
data class GameRecord(
    val nickname: String = "",
    val topic: String,           // 주제
    val score: Int,              // 점수 (0-100)
    val correctCount: Int,       // 정답 수
    val totalQuestions: Int,     // 전체 문제 수
    val timestamp: Long = System.currentTimeMillis() // 게임 시간
)

//SharedPreferences에 게임 기록 저장/불러오기
object GameRecordManager {
    private const val PREFS_NAME = "quiz_records"
    private const val KEY_RECORDS = "game_records"

    fun saveRecord(context: Context, record: GameRecord) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val records = loadRecords(context).toMutableList()
        records.add(record)

        val gson = Gson()
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_RECORDS, json).apply()
    }

    fun loadRecords(context: Context): List<GameRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RECORDS, null) ?: return emptyList()

        val gson = Gson()
        val type = object : TypeToken<List<GameRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun clearRecords(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECORDS).apply()
    }
}

@Composable
fun QuizApp(){

    var currentScreen by remember { mutableStateOf("home") }
    var currentNickname by remember { mutableStateOf("") }   // ⭐ 이번 게임 닉네임
    var pendingTopic by remember { mutableStateOf("") }       // 선택한 주제명
    var pendingFileName by remember { mutableStateOf("") }

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
                pendingTopic = topicName
                pendingFileName = fileName
                currentScreen = "name"
            },
            onWrongQuizClick = {
                currentScreen = "wrong"
            },
            onRankingClick = {
                currentScreen = "ranking"
            }
        )

        "name" -> NameScreen(
            onConfirm = { nickname ->
                currentNickname = nickname
                currentTopic = pendingTopic
                currentQuizList = loadQuizFromAssets(context, pendingFileName)
                currentScreen = "quiz"
            },
            onCancel = {
                currentScreen = "home"
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
                lastWrongList = (lastWrongList + wrongList).distinct() // 중복 제거
                currentScreen = "result"
            }
        )
        "result" -> ResultScreen(
            nickname = currentNickname,
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
        "ranking" -> RankingScreen(
            onBackToHome = { currentScreen = "home" }
        )
    }
}

@Composable
fun HomeScreen(
    context: Context,
    onTopicSelected: (String, String) -> Unit,
    onWrongQuizClick: () -> Unit,
    onRankingClick: () -> Unit
) {
    // 그라데이션 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFD1E4FF), Color.White)
                )
            )
            .padding(horizontal = 24.dp, vertical = 30.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 상단 제목
            Text(
                text = "Quiz App",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // 2×2 Grid
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {


                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),

                ) {
                    HomeMenuCard(
                        iconRes = R.drawable.general,    // 사자성어
                        title = "사자성어 퀴즈",
                        onClick = { onTopicSelected("사자성어", "idioms.json") }
                    )
                    HomeMenuCard(
                        iconRes = R.drawable.capital,    // 수도
                        title = "수도 퀴즈",
                        onClick = { onTopicSelected("수도", "capitals.json") }
                    )
                }

                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeMenuCard(
                        iconRes = R.drawable.nonsense,  // 넌센스
                        title = "넌센스 퀴즈",
                        onClick = { onTopicSelected("넌센스", "nonsense.json") }
                    )
                    HomeMenuCard(
                        iconRes = R.drawable.general,    // 상식
                        title = "상식 퀴즈",
                        onClick = { onTopicSelected("상식", "general.json") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 오답노트 버튼
            HomeLargeButton(
                iconRes = R.drawable.check,
                text = "오답노트",
                onClick = onWrongQuizClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 랭킹 버튼
            HomeLargeButton(
                iconRes = R.drawable.ranking,
                text = "랭킹",
                onClick = onRankingClick
            )

        }
    }
}

@Composable
fun NameScreen(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F1FF)),   // 배경색 E7F1FF
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "닉네임을 입력하세요",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4880EE)      // 글자색 4880EE
            )

            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.material3.TextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예) 가나디") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 취소 버튼
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text("취소", color = Color.Black)
                }

                // 시작하기 버튼
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onConfirm(nameInput.trim())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4880EE)
                    )
                ) {
                    Text("시작하기", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

//게임 종류 선택 카드
@Composable
fun HomeMenuCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(90.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

//오답노트, 랭킹 버튼
@Composable
fun HomeLargeButton(
    iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(320.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
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
            .background(Color(0xFFE7F1FF))
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ){
        Box( // 주제와 뒤로가기
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA), shape = RoundedCornerShape(8.dp))
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
                Text("⌂", fontSize = 24.sp, color = Color(0xFF4880EE))
            }
            Text( //주제 텍스트
                text = topic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4880EE),
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
                .background(Color(0xFF3383F9), shape = RoundedCornerShape(8.dp))
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
                fontSize = 12.sp,
                color = Color.White,
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

                val (bgColor, borderColor, textColor) = when {
                    // 정답 선택
                    isAnswerRevealed && isSelected && index == currentQuiz.answer -> Triple(
                        Color(0xFFEBF4FF),   // 배경: EBF4FF
                        Color(0xFF3383F9),   // 테두리: 3383F9
                        Color(0xFF3383F9)    // 글자: 3383F9
                    )

                    // 오답 선택
                    isAnswerRevealed && isSelected && index != currentQuiz.answer -> Triple(
                        Color(0xFFFFEFF1),   // 배경: FFEFF1
                        Color(0xFFF24554),   // 테두리: F24554
                        Color(0xFFF24554)    // 글자: F24554
                    )

                    // 평소 상태
                    else -> Triple(
                        Color.White,         // 배경: 흰색
                        Color(0xFFFFFFFF),   // 테두리: 연한 회색
                        Color.Black          // 글자: 검정
                    )
                }

//                val backgroundColor = when{
//                    isAnswerRevealed && isSelected  && index == currentQuiz.answer -> Color(0xFFEBF4FF) // 정답이면 초록색
//                    isAnswerRevealed && isSelected  && index != currentQuiz.answer -> Color(0xFFFFEFF1) // 오답이면 빨강
//                    else -> Color.White
//                }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(
                            width = 2.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row (verticalAlignment = Alignment.CenterVertically){
                        Text("${index+1}. ", fontSize = 16.sp, color = textColor,  fontWeight = FontWeight.SemiBold)
                        Text(option, fontSize = 16.sp, color = textColor,  fontWeight = FontWeight.SemiBold)
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
    nickname: String,
    topic: String = "",
    totalQuestions: Int = 0,
    wrongCount: Int = 0,
    onBackToHome: () -> Unit,
    onWrongQuiz: () -> Unit
){
 // 점수 표시, 홈 버튼
    val context = LocalContext.current

    // 0문제일 때 0 나누기 방지
    val safeTotal = if (totalQuestions == 0) 1 else totalQuestions
    val correctCount = totalQuestions - wrongCount
    val score = (correctCount.toFloat() / safeTotal * 100).toInt()

    LaunchedEffect(topic, totalQuestions, wrongCount) {
        if (totalQuestions > 0) {
            val record = GameRecord(
                nickname = nickname,
                topic = topic,
                score = score,
                correctCount = correctCount,
                totalQuestions = totalQuestions
            )
            GameRecordManager.saveRecord(context, record)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F1FF)),
        contentAlignment = Alignment.Center
    ) {
        // 안쪽 카드
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🏆 트로피 이미지 (ranking.png)
            Image(
                painter = painterResource(R.drawable.ranking),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 주제 + "퀴즈 완료!"
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF4880EE),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(topic)
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(" 퀴즈 완료!")
                    }
                },
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ 문제풀이/정답/오답 + 최종 점수 박스
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFDFD), RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("문제풀이 수", fontSize = 12.sp, color = Color(0xFF777777))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$totalQuestions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("정답 수", fontSize = 12.sp, color = Color(0xFF777777))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$correctCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("오답 수", fontSize = 12.sp, color = Color(0xFF777777))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$wrongCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ⭐ 점수 표시 색 : 4880EE
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("최종 점수 : ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF4880EE),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("$score")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("점")
                            }
                        },
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔵 홈 / 오답보기 버튼 (색: 4880EE)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = onBackToHome,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4880EE)
                    )
                ) {
                    Text(
                        text = "홈",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onWrongQuiz,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4880EE)
                    )
                ) {
                    Text(
                        text = "오답보기",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
    //정렬 상태
    var isNewestFirst by remember { mutableStateOf(true) }
    //정렬된 리스트
    var sortedList = if (isNewestFirst) wrongQuizList else wrongQuizList.reversed()
 // 틀린 문제 목록 표시
    Column (
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFFFFF)).verticalScroll(rememberScrollState())
    ){
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "틀린 문제 다시보기",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,

            )
            Spacer(modifier = Modifier.weight(1f))

            Button( //홈으로 돌아가기 버튼
                onClick = onBackToHome,
                contentPadding = PaddingValues(0.dp), //버튼 내부 여백 제거
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.size(48.dp).padding(top = 20.dp, end = 20.dp)
            ) {
                Text("⌂", fontSize = 24.sp, color = Color.Black)
            }
        }
        //Spacer(modifier = Modifier.height(16.dp))

        //최신순과 오래된순 토글
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            //Spacer(modifier = Modifier.weight(1f))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1F4EF5),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append("정답") }
                    append(" / ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFFF24554),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) { append("내가 고른 답") }
                },
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { isNewestFirst = !isNewestFirst }
            ) {
                Text(
                    text = if (isNewestFirst) "최신순" else "오래된순",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painterResource(R.drawable.pin),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        //정렬된 틀린 문제 목록
        if(wrongQuizList.isEmpty()){
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ){
                Text("틀린 문제가 없습니다!", fontSize = 18.sp)
            }
        }else{
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                //contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                sortedList.forEach { quiz ->

                    // 흰색 카드 + drop shadow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0x40000000), // 25% 블랙
                                spotColor = Color(0x40000000)
                            )
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(vertical = 16.dp, horizontal = 16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 주제 표시 pill
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFEBF4FF),
                                            RoundedCornerShape(40.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = quiz.topic,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1F4EF5),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = quiz.question,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))


                                Image(
                                    painter = painterResource(R.drawable.trash),
                                    contentDescription = "삭제",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onDeleteQuiz(quiz) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 보기들
                            quiz.options.forEachIndexed { i, option ->
                                val color = when {
                                    // 정답 (파란색)
                                    i == quiz.answer -> Color(0xFF1F4EF5)
                                    // 내가 고른 오답 (빨간색)
                                    i == quiz.selectedAnswer -> Color(0xFFF24554)
                                    else -> Color.Black
                                }

                                Text(
                                    text = "${i + 1}) $option",
                                    color = color,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RankingScreen(
    onBackToHome: () -> Unit
){
    val context = LocalContext.current

    // 저장된 기록 불러오기
    var records by remember {
        mutableStateOf(GameRecordManager.loadRecords(context))
    }

    // 점수/정답/시간 순으로 정렬
    val sortedRecords = remember(records) {
        records.sortedWith(
            compareByDescending<GameRecord> { it.score }
                .thenByDescending { it.correctCount }
                .thenByDescending { it.timestamp }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F1FF))                 // 배경색 E7F1FF
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 홈 버튼 (오른쪽 상단 작은 아이콘 정도)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onBackToHome,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text("⌂", fontSize = 24.sp, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 왕관 이미지
        Image(
            painter = painterResource(R.drawable.crown),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RANKING",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (sortedRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 플레이 기록이 없어요!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(sortedRecords) { index, record ->

                    //s 순위별 배경색/글자색 설정
                    val (bgColor, mainTextColor, subTextColor) = when (index) {
                        0 -> Triple(Color(0xFF1F4EF5), Color.White, Color(0xFFEFEFFF)) // 1위
                        1 -> Triple(Color(0xFF4880EE), Color.White, Color(0xFFEFEFFF)) // 2위
                        2 -> Triple(Color(0xFF83B4F9), Color.Black, Color(0xFF222222)) // 3위
                        else -> Triple(Color.White, Color.Black, Color(0xFF555555))    // 나머지
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = bgColor,
                                shape = RoundedCornerShape(24.dp)   // corner radius 24
                            )
                            .padding(vertical = 16.dp, horizontal = 20.dp)
                    ) {
                        Column {
                            // 윗줄: 순위 + 닉네임 / 점수
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}위 ${record.nickname}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${record.score}점",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 아랫줄: 주제 / 정답 수
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = record.topic,
                                    fontSize = 14.sp,
                                    color = subTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "정답: ${record.correctCount} / ${record.totalQuestions}",
                                    fontSize = 14.sp,
                                    color = subTextColor
                                )
                            }

                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
