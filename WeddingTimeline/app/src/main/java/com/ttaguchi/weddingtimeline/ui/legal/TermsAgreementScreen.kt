package com.ttaguchi.weddingtimeline.ui.legal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api

// constを削除してvalにする（trimIndent()は定数式ではないため）
internal val TERMS_TEXT = """
WeddingTimeline 利用規約・コミュニティガイドライン

最終更新日：2025年12月18日

本規約は、WeddingTimeline（以下「本アプリ」）の利用条件およびコミュニティガイドラインを定めるものです。本アプリをインストールまたは利用することで、ユーザーは本規約に同意したものとみなされます。

⸻

第1条（本アプリの概要）
    1.	本アプリは、結婚式や関連イベントに参加するゲスト同士で、写真・動画・テキスト等の投稿（以下「投稿コンテンツ」）を共有するタイムラインアプリです。
    2.	本アプリは、ルームIDおよびルームキーによる「ルーム」単位での限定公開を前提としており、特定のルームに参加したユーザーのみが、そのルーム内のタイムラインを閲覧・投稿できます。

⸻

第2条（適用範囲）
    1.	本規約は、本アプリの利用に関わる一切の関係に適用されます。
    2.	運営者は、必要に応じて本規約とは別に利用上のルール（コミュニティガイドライン等）を定める場合があります。その場合、当該ルールは本規約の一部を構成するものとします。

⸻

第3条（アカウントおよび認証）
    1.	本アプリは、Firebase Authentication 等による匿名認証もしくは外部サービス連携によりユーザーを識別します。
    2.	ユーザーは、表示名・アイコン等のプロフィール情報を任意に設定できますが、第三者の権利を侵害したり、なりすましに該当する名称・画像等を使用してはなりません。
    3.	ユーザーには、識別のため @UID先頭4桁 などのタグが付与される場合があります。これはルーム内での識別・トラブル対応のために利用されます。

⸻

第4条（ルームID・ルームキーの管理）
    1.	ルームIDおよびルームキーは、招待されたゲストのみに共有されることを前提としています。
    2.	ユーザーは、ルームID・ルームキーを第三者に無断で再配布したり、SNS等で公開してはなりません。
    3.	ルームID・ルームキーの管理不備により発生したトラブルについて、運営者は故意または重大な過失がない限り責任を負いません。

⸻

第5条（投稿コンテンツの権利と利用）
    1.	ユーザーが本アプリに投稿した写真・動画・テキスト等の著作権その他の権利は、原則としてユーザー自身に帰属します。
    2.	ユーザーは、運営者に対し、以下の目的の範囲内で投稿コンテンツを無償・非独占的に利用する権利を許諾するものとします。
    •	本アプリおよび関連サービスの提供・運営・改善
    •	投稿コンテンツの保存、表示、サムネイル生成等の技術的処理
    3.	運営者は、ユーザーの同意なく投稿コンテンツを対外的な宣伝目的等に利用しません（ただし、法令に基づく開示を除きます）。

⸻

第6条（禁止事項）

ユーザーは、本アプリの利用にあたり、以下の行為をしてはなりません。
これらに該当すると運営者が判断した場合、予告なく投稿の削除・アカウント制限等の対応を行うことがあります。
    1.	法令または公序良俗に反する行為
    2.	犯罪行為、またはそれを助長・示唆する行為
    3.	他者の著作権、肖像権、商標権、プライバシー権、その他の権利・利益を侵害する行為
    4.	参加者・新郎新婦・式場スタッフ等の写真や個人情報を、本人の合理的な同意なく公開する行為
    5.	過度に露骨な性表現、暴力的な表現、差別的・誹謗中傷的な表現等の不適切なコンテンツの投稿
    6.	スパム投稿、同一内容の連続投稿、広告・宣伝のみを目的とした投稿
    7.	ルームID・ルームキーや、他ユーザーの機密情報をスクリーンショット等で共有・第三者に開示する行為
    8.	なりすまし、虚偽のプロフィールの使用、他人のアカウントの不正利用
    9.	本アプリの運営・ネットワーク・セキュリティを妨害する行為
    10.	その他、運営者が不適切と判断する行為

⸻

第7条（通報・ミュート・運営による対応）
    1.	本アプリは、ユーザーが不適切な投稿やユーザーを通報できる機能を提供する場合があります。
    2.	ユーザーは、タイムライン上で不快な投稿・ユーザーがいた場合、通報機能やミュート・ブロック機能を利用できます。
    3.	運営者は、通報内容やログ等を確認のうえ、必要に応じて以下の対応を行うことがあります。
    •	投稿コンテンツの削除・非表示
    •	アカウントの一時停止・利用制限
    •	重大な違反の場合のアカウントの恒久的な停止・ルームからの退室処理
    4.	運営者は、ユーザー間のトラブルについて、可能な範囲で解決に努めますが、全てのトラブルについて解決を保証するものではありません。

⸻

第8条（アカウント削除・データの取り扱い）
    1.	ユーザーは、アプリ内の「アカウント削除」等の機能から、自身のアカウント削除を申請できます。
    2.	アカウント削除が完了した場合、当該ユーザーのプロフィール情報・認証情報は削除されますが、以下の情報は完全には削除されない場合があります。
    •	他ユーザーとのやり取りの整合性を保つために必要な情報
    •	法令上の義務に基づき保存が必要なログや記録
    3.	アカウント削除後も、法令に基づく請求や運営者の正当な権利行使のために、一定期間ログを保持する場合があります。

⸻

第9条（免責事項）
    1.	運営者は、本アプリの提供にあたり、バグや不具合が存在しないこと、ならびに特定の目的への適合性を保証するものではありません。
    2.	通信状況やシステム障害等により、本アプリが正常に利用できない場合があります。これによりユーザーに損害が生じても、運営者は故意または重大な過失がない限り責任を負いません。
    3.	ユーザー間のトラブル（投稿内容をめぐる紛争、権利侵害、誹謗中傷等）については、当事者間で解決するものとし、運営者は合理的な範囲で協力しますが、一切の責任を負うものではありません。

⸻

第10条（規約の変更）
    1.	運営者は、必要に応じて本規約を変更することができます。
    2.	本規約を変更する場合、アプリ内画面やWebサイト等にて、変更後の内容と効力発生日を事前に告知します。
    3.	ユーザーが、規約変更後に本アプリを継続利用した場合、変更後の規約に同意したものとみなします。

⸻

第11条（準拠法・管轄）
    1.	本規約の解釈・適用については、日本法を準拠法とします。
    2.	本アプリに関して運営者とユーザーとの間に紛争が生じた場合、運営者の所在地を管轄する日本の裁判所を第一審の専属的合意管轄裁判所とします。

⸻

コミュニティガイドライン（要約）

WeddingTimeline は、結婚式という特別な時間を共有するためのアプリです。みんなが安心して使えるよう、以下のルールを守ってご利用ください。

1. リスペクトと思いやり
    •	新郎新婦・ゲスト・式場スタッフなど、全ての関係者を尊重しましょう。
    •	誹謗中傷・悪口・からかい・過度に攻撃的なコメントは控えてください。
    •	個人が特定できる情報（住所・電話番号・メールアドレス等）は投稿しないでください。

2. 写真・動画の取り扱い
    •	顔がはっきり写っている写真や動画は、写っている本人が不快にならないかを意識して投稿してください。
    •	明らかに嫌がる人が写っているもの、酔って判断能力が低い状態の人を面白半分で撮影したものなどは投稿しないでください。
    •	式場のルールや撮影禁止エリアがある場合は、それに従ってください。

3. 不適切なコンテンツの禁止

以下のような内容は投稿しないでください。
    •	暴力的・グロテスク・性的に露骨な表現
    •	差別的・侮辱的な発言、過度な下ネタ
    •	著作権を侵害する画像・音源・動画など（市販映画・テレビ番組の映像など）
    •	宣伝・勧誘・マルチ商法・出会い系など、結婚式と無関係な営利目的の投稿
    •	ルームID・ルームキーを含むスクリーンショットや、他人の機密情報

4. ルームの安全を守る
    •	ルームID / ルームキーは、招待された人だけが使うことを前提としています。
    •	SNSや不特定多数が見る場所に、ルームID・ルームキーを掲載しないでください。
    •	間違って公開してしまった場合は、速やかに新郎新婦またはルーム作成者に連絡してください。

5. 困ったときは通報・ミュート
    •	不快な投稿やユーザーを見つけた場合は、通報機能を使って運営へ知らせてください。
    •	個人的に見たくないユーザーがいる場合は、ミュート・ブロック機能を活用してください。
    •	通報内容をもとに、運営者が投稿削除やアカウント制限などの対応を行う場合があります。

6. 規約違反への対応
    •	上記ガイドラインや利用規約に違反する行為が確認された場合、運営者は次のような対応を行うことがあります。
    •	投稿コンテンツの削除・非表示
    •	一時的な機能制限
    •	ルームからの退室処理
    •	アカウントの停止・削除
    •	悪質な場合は、関係機関への通報等を含め、適切な法的措置を検討します。

⸻

お問い合わせ

運営者：田口 友暉（個人開発）
連絡先：ttaguchidevelop@gmail.com
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAgreementScreen(
    onAgree: () -> Unit,
) {
    var isChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "利用規約・ガイドライン") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = TERMS_TEXT,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "上記の利用規約・コミュニティガイドラインに同意します。")
            }

            Button(
                onClick = onAgree,
                enabled = isChecked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "同意してはじめる")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
