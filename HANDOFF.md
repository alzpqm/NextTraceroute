# NextTraceroute 接手檔

> **開工規則：每次開始修改前必須完整讀完本檔；每次結束前必須更新「目前工作」與「驗證紀錄」。**
> 不可只依賴聊天記憶，也不可把 PIN、簽署密碼、Token、真實姓名或真實電子郵件寫入專案、提交、建置產物或發佈資訊。

## 專案與 Git

- 工作目錄：以目前 Git repository 根目錄為準；不可把本機絕對路徑寫入可公開檔案。
- 上游：`origin` → `https://github.com/nxtrace/NextTraceroute.git`
- 維護 Fork：`fork` → `https://github.com/alzpqm/NextTraceroute.git`
- 目前開發分支：`codex/nexttrace-1.7.2-ui`
- 目前穩定版：`v0.2.4`，release commit `9c93928`，GitHub Release：`https://github.com/alzpqm/NextTraceroute/releases/tag/v0.2.4`。
- 發佈與提交只能使用 GitHub 帳號名稱，以及 GitHub 提供的 noreply email；不可出現真名或真實 email。
- 每次 push、tag 或 GitHub Release 前都必須完成隱私掃描。發現本機使用者名稱、絕對路徑、裝置序號、PIN、Token、密碼、私鑰、keystore、真實姓名或真實 email 時禁止發佈。
- 隱私掃描必須涵蓋工作樹、待推送 commits、tag/commit 作者資料、APK/AAB 簽章憑證與 Release 中繼資料；掃描結果須更新在本檔。
- 未經本輪明確要求，不建立 GitHub Release；正式版只能在完整檢查未發現阻擋問題後發佈。
- 不覆蓋或刪除不明的未提交檔案。2026-09-02 開工時存在兩個未追蹤目錄：
  - `app/src/main/res/drawable-v24/`
  - `app/src/main/res/mipmap-anydpi-v26/`
  先確認來源與用途，再決定是否納入版本控制。

## Android 與版本基線

- `compileSdk = 37`
- `targetSdk = 37`
- `minSdk = 26`（Android 8.0；舊於此版本不支援）
- Java / Kotlin JVM target：21
- 現行正式版：`versionName = 0.2.4`、`versionCode = 22`
- 後續公開版本必須高於 0.2.4，不可重複既有版本號或造成比原版更舊的觀感。
- UI 基線：Jetpack Compose + Material 3，需兼顧 Android 17 / API 37 的設計與行為。

## NextTrace 後端

- 客戶端已對齊 NextTrace core/API `v1.7.2`。
- 使用 legacy v3 WebSocket / PoW API 流程；預設不需要使用者 API token。
- 不應重新加入會誤導一般使用者的 API token 設定，除非未來後端協定明確要求。

## 測試環境

- API 37 模擬器：`NextTraceroute_API_37`，ARM64、16K page size。
- Android 14 實機可透過 `adb devices` 在本機辨識；裝置序號與解鎖資訊是敏感資料，不得記錄在此檔或任何提交中。
- Debug application ID：`com.surfaceocean.nexttraceroute.debug`，可與正式版並存。
- macOS 命令列可能預設到 Java 8；建置時必須使用 Android Studio 內建 JBR 21，並設定 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`。不可把實際本機路徑寫進 repository。
- 本機正式簽署金鑰的位置索引與鑰匙圈服務名稱已記錄在 `.git/info/nexttraceroute-private-handoff.md`；該檔僅存在本機且不會被提交。後續簽署前先讀該檔，不可重新搜尋或輸出金鑰位置與密碼。
- Fork 已設定正式簽署所需的四個加密 Actions Secrets；`.github/workflows/build.yml` 的「手動簽署建置」可直接產生與 0.2.1 相同憑證的 APK／AAB。API 37 平台的正確 SDK 套件 ID 是 `platforms;android-37.0`。
- 常用驗證：
  - `./gradlew testDebugUnitTest lintDebug assembleDebug`
  - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
  - 測試輸入、清除、IME 動作、鍵盤開合、旋轉、深色模式與實際路由追蹤。

## 目前工作（2026-10-05，0.2.4 已完成發佈）

- 使用者已明確要求修正、測試及發佈 GitHub，並要求發佈前隱私與語言檢查；本輪發佈授權已取得，不需沿用前輪未授權的結論。
- 已修正三項實機確認的問題：異常數值造成設定頁崩潰、後端不可用時遺失本地分類、查詢期間才出現的跳點缺少 metadata。新增地圖請求取消、10 秒整體逾時與 8 KiB 回應上限。
- 依使用者選擇，中文（正體中文）為預設與第一個介面語言，英文（英式英文，en-GB）為第二個；中文介面選項簡稱「中文／英文」。API 回應語言獨立保留，兩種介面資源均隨 APK／AAB 提供。
- 0.2.4 已正式發佈並設為 latest，包含 APK、AAB 與 SHA256SUMS；tag 指向已通過簽署建置的 `9c93928`。24 項 JVM、兩平台各 13 項回歸、正式升級、來源／產物隱私、匿名提交／tag、憑證及 GitHub 遠端資產雜湊均已核對。
- README 已更新為 0.2.4 與中文「開始／停止」操作，首頁截圖來自無個人資料的 API 37 正式版模擬器。測試報告及版本說明位於 `docs/BUG_AUDIT_2026-10-04.md` 與 `docs/releases/v0.2.4.md`。
- 簽署沿用 GitHub Actions 的既有 Secrets 與匿名憑證，不在 Windows 建立新金鑰或輸出密碼。保留手機正式版資料，異常設定 fixture 僅能操作 Debug 套件。
- 既有 `scripts/privacy-scan.sh` 的 mode-only 未提交變更保留，不納入本輪 commit。禁止同一工作目錄平行執行不同 Gradle 建置。
- 實機完成升級、資料核對、中文冷啟動及設定頁檢查後，無線 ADB 中斷；重連未成功，未據此推定使用者操作或 App 崩潰。正式版資料未清除；本輪曾使用的手機 `/data/local/tmp/nexttraceroute-release-ui.xml` 僅為本機 UI 暫存，斷線後無法再清理，後續連線可只移除此檔，不可擴大刪除範圍。模擬器的本輪 UI 暫存已清除、夜間模式已恢復原值。

### 同日較早的審查階段（保留歷史）

- 本輪為 0.2.3 發佈後的 bug 審查與 Android 14／API 34 實機回歸；已完整讀取接手檔，未修改正式功能、版本號、簽署或公開發佈資訊。
- 已確認三個待修問題：地理資訊處理會漏掉查詢等待期間才出現的跳點；後端不可用時連本地保留位址分類也缺失；異常設定值 `NaN` 會導致設定頁崩潰。詳細證據與重現方式見 `docs/BUG_AUDIT_2026-10-04.md`。
- 已新增 Debug-only 操作與受控連線測試，保留原有正式 App 與資料。重現測試會在未修正的 0.2.3 上失敗，不能把它們忽略後宣稱所有回歸通過。
- USB 安裝與指令曾遭遇傳輸中斷；依使用者要求改用已授權的無線 ADB，連線位址、裝置識別資料與完整系統記錄均不寫入專案。手機前景畫面切換不能作為使用者操作的證據。
- 異常設定測試若被系統終止，`AuditSettingsRule` 的 finally 不會執行；恢復 Debug 專用備份／不存在標記後，已確認無殘留測試設定且可正常冷啟動。不可刪除或修改正式 App 設定及歷史資料。
- 後續先取得修正要求，修正以上問題並重新執行回歸；本輪未要求發佈新版本，不建立 GitHub Release。

### 前輪工作（2026-09-20，保留歷史）

- 本輪已完成長網址解析與頁面崩潰風險修正，0.2.3／versionCode 21 已正式發佈並設為 latest；APK、AAB 與 SHA256SUMS 均已完成遠端核對。
- 輸入改為先擷取 authority，再以有長度上限的方式驗證；貼上 URL 立即轉為主機名稱，移除可觸發 StackOverflowError 的遞迴網域 regex。
- DNS A／AAAA 集中合併去重，支援 CNAME-only 回覆及循環防護；WebSocket 回覆先檢查 IP、型別與空值，再由主執行緒更新畫面。
- 追蹤停止／重跑改為取消並等待前一輪工作；ping 可中斷、WebSocket 關閉、每跳查詢限制同時執行數量。歷史紀錄讀寫及外部 Intent 加入錯誤處理。
- 本輪使用 Windows JDK 21 與 Android 37 x86_64／16 KB 模擬器；正式簽署沿用 GitHub Actions Secrets，不建立新金鑰。
- 使用者已明確要求正式發佈並完成 GitHub CLI 授權；本輪回歸、正式產物簽章與隱私核對均完成。後續每次發佈仍須重新取得本輪發佈要求並執行檢查。

- 首頁輸入框跳動已修正：移除浮動 label、固定 64 dp 高度、永久保留清除鍵槽位、單次更新輸入狀態、避免每次按鍵 `trim()`，並固定 Run 按鈕高度。
- API 37 UI hierarchy 已確認空白聚焦、第一字、完整文字與清空後，輸入框皆為 `[68,358][768,526]`，Run 文字皆為 `[905,416][970,469]`；第一字出現時版面 bounds 不變。
- Placeholder 已縮短為 `Domain, IP or URL`，避免一般手機寬度截斷。
- README 與隱私權政策已改為正體中文；舊商店徽章、舊聯絡方式、舊 FAQ 與舊 UI 截圖已移除，改用 API 37 新截圖。
- GitHub repository 描述、0.2.0 Release notes 與已淘汰預覽版 Release notes 已改為正體中文，Issues 已啟用。尚未建立新 Release。
- 原始碼與 LICENSE 保留 `surfaceocean` 著作權署名，但已移除 email；目前維護入口指向 `alzpqm/NextTraceroute`，並保留上游連結。
- 已加入 `scripts/privacy-scan.sh`；發佈來源前執行無參數模式，建置正式 APK/AAB 後執行 `--artifacts`。
- 0.2.1 正式版已完成建置、發佈與遠端核實；後續繼續研究 UI、設定頁面與未解 bugs，下一個公開版本必須高於 0.2.1。
- 0.2.2 已修正日／夜模式狀態錯誤：應用程式顏色固定來自當前 Material 3 `colorScheme`，舊版 `settings.json` 的 11 個顏色欄位會被忽略，重新儲存時也會自動移除；系統主題切換後不再被舊設定覆蓋。
- 設定頁已重構為 Material 3 區塊式版面，改用適當的 surface／on-surface 色階、垂直配置 Slider、外置輸入欄標籤與較低對比分隔線；已移除選色器、Dark／Light Preset 與 `compose-color-picker` 依賴。
- 已移植上游 0.1.7 的重複 IP 防崩潰修正，位址選擇清單改以去重後資料與穩定 key 顯示，不回退本分支較新的依賴、API 37 或版本基線。
- `.github/workflows/build.yml` 已改為僅能手動啟動的簽署建置，不再由 Release 事件觸發或覆蓋正式資產；工作流程只產生保留一天的暫存 artifact，正式發佈前仍須下載至本機重新核對。
- 0.2.2 正式版已發佈；APK、AAB 與 `SHA256SUMS.txt` 均已上傳，GitHub 遠端雜湊完成核對。後續公開版本必須高於 0.2.2。

## 驗證紀錄

- 2026-10-05，0.2.4 發佈收尾：GitHub Actions run `37180588454` 在 `9c93928` 成功完成正式建置、測試、lint、Bash 來源／產物隱私掃描及暫存上傳。最終程式碼在 Android 14 實機、API 37 模擬器各再次取得 `OK (13 tests)`。
- 2026-10-05：APK 的 153 個、AAB 的 162 個非空 ZIP entries 均在本機再次逐項掃描，包括 UTF-8／UTF-16、本機身分與路徑、裝置識別資料、私人 email、token 及私鑰樣式，無阻擋項目。公開來源、發佈文字、匿名 commit／tag、Release 作者及資產上傳者均已核對；GitHub Secret Scanning 為 0 open alerts。
- 2026-10-05：APK、AAB 憑證均為 `CN=alzpqm, O=alzpqm`，SHA-256 `b31af4586d6222c20bfaf0af6b6359023818714ddf57b022fdea67e115d223a9`，與既有正式版一致。APK 驗章及 16 KB zipalign 通過；AAB 的 jarsigner 驗章成功，仍有自簽、無 timestamp、ZIP 屬性及串流 manifest 順序提示。未驗證 Google Play 上傳。
- 2026-10-05：手機與模擬器均由正式 0.2.3 以覆蓋安裝升級至 0.2.4／versionCode 22，minSdk 26、targetSdk 37。手機升級前後、首次開啟新版前，既有資料檔雜湊完全一致；未解除安裝或清除資料。手機中文首頁／設定頁成功，模擬器正式包回環追蹤完成、顯示 RFC1122 及複製結果；當次 App 程序 crash buffer 無 fatal 記錄。
- 2026-10-05：中文深色設定、英文淺色設定與歷史頁完成視覺檢查，文字與控制項未見重疊。語言切換在 Activity 重建與冷啟動後保留；介面與 API 語言選項分離。
- 2026-10-05：0.2.4 APK SHA-256 `707bef4d366e7386c389c90304b6e859c82d0aa0dd59a0cff7eb5c8fe9f7ea07`；AAB `ad423bdb5d33a9b04b0ecf467960b04f16be6b3e0e8140bdb5ef919c68a1113f`；SHA256SUMS 檔 `d6bbc034e9f4f6e52c31d400917baf713143934985be259b3512cb6b199dd970`。三個 GitHub assets digest 均一致；Release 非 draft／prerelease 且為 latest，遠端 tag 指向 `9c93928`，版本說明與審查檔案一致。

- 2026-10-04，0.2.4：`testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` 成功；24 項 JVM 測試通過，lint 為 0 errors、11 warnings（依賴更新與既有資源配置）。沒有新增依賴。
- 2026-10-04，0.2.4：Android 14／API 34／4 KB 實機使用獨立 ADB 服務的無線連線；API 37／x86_64／16 KB 模擬器使用另一 transport。兩者均取得 `OK (13 tests)`，三項缺陷重現測試與中文／英式英文切換、Activity 重建全部通過。
- 2026-10-04，0.2.4：來源隱私掃描包含公開檔案、私鑰／token 樣式、私人 email、本機身分與路徑、同步副本及完整歷史的維護者作者資料，未發現阻擋項目；GitHub Secret Scanning 為 0 open alerts。正式 APK／AAB 與 Release 中繼資料仍須在簽署後另行核對。

- 2026-10-04：GitHub Fork 沒有新增 issue；latest 仍為 0.2.3。上游最新提交仍為 2026-08-14 的 0.1.7，未發現新的上游修正可直接套用。
- 2026-10-04：Android 14／API 34 實機、4 KB page size、root 可用；正式套件為 0.2.3／versionCode 21，Debug 與正式版並存。原有 4 項 UI 回歸全部通過。
- 2026-10-04：新增 7 項實機補測有 5 項通過、2 項已知缺陷失敗。通過項目為 IPv6 回環、DNS 失敗後重跑、追蹤中旋轉及停止／重建、設定儲存及重載、歷史詳細資訊往返；失敗項目分別重現本地分類依賴後端及跳點到達時序缺漏。
- 2026-10-04：異常設定的 instrumentation 測試被系統以 ANR 終止，不能視為一般 App 使用的確定原因。另以不經測試框架的 Debug 冷啟動／設定頁操作，直接取得 `IllegalArgumentException: Cannot round NaN value`，定位 `Settings.kt` 的 `NumberSliderSetting`。
- 2026-10-04：使用 `--rerun-tasks` 實際重新執行 18 項 JVM 測試，全部通過；修正本輪測試 fixture 的 Compose state 宣告後，lint 為 0 errors、11 warnings。警告為依賴更新及既有資源目錄配置，不是新的功能錯誤。
- 2026-10-04：重建 androidTest APK 後，用獨立本機 ADB 服務的無線 transport 再跑 3 項複核：正常設定儲存／冷啟動通過，兩個 metadata 缺陷均再次得到相同失敗斷言。已檢查沒有異常設定或恢復標記殘留，清除本輪 UI hierarchy 暫存，Debug 首頁冷啟動成功。
- 2026-10-04：來源靜態隱私檢查在本機以等價 PowerShell／ripgrep 規則執行，本機使用者路徑、私鑰、憑證／簽署檔案、同步副本及不允許的 email 均為 0 matches；`git diff --check` 通過。本輪未新增 commit 或正式簽署產物，因此這不是新版本完整發佈隱私驗證。
- 2026-10-04：正式 App 版本與資料保留；已恢復 Debug 異常設定，未提交、push、tag、簽署或發佈。既有 `scripts/privacy-scan.sh` 的 mode-only 未提交變更保留、不納入本輪內容。

- 2026-09-20：確認使用者提供的 CDN 主機可正常擷取及解析，未將其臨時 URL 參數寫入測試或文件；另以長網域穩定重現舊 regex 的 StackOverflowError。
- 2026-09-20：提交 `840c50b` 已完成乾淨 `clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`，90 個 tasks 成功。18 個 JVM 測試與 4 個 API 37／16 KB 操作測試全部通過；lint 為 0 errors、5 warnings。涵蓋十萬字元 URL、過長網域、無效／等價 IPv6、CNAME 循環與去重、異常 API JSON、完整回環追蹤、四次快速停止重跑與 Activity 重建。
- 2026-09-20：使用者提供的 CDN 網址已在 App 中成功解析，清除輸入及 IME Go 操作通過；查核模擬器 crash buffer 未見本 App 崩潰或 ANR。首輪測試揭露完成前提前顯示複製結果按鈕的時序差，已修正並通過最終回歸。
- 2026-09-20：建置資料夾仍有舊同步產生的 ` 2` 副本，已透過 Gradle clean 清除可再生產物。曾有兩個本機 Gradle 建置互相覆寫輸出而失敗；改為單一乾淨建置後成功，後續禁止在同一工作目錄並行啟動不同 Gradle 建置。
- 2026-09-20：GitHub 手動簽署工作流程 run `35500735918` 在提交 `840c50b` 上成功，正式 APK／AAB 通過原有 Bash 來源／產物隱私掃描。CI 淺層 checkout 跳過提交身分項目，已在本機完整 history 補核；commit、tag 與 Release 作者皆為匿名 GitHub 身分。
- 2026-09-20：本機再次解壓逐項掃描 APK 的 153 個與 AAB 的 162 個非空 entries，未發現私有路徑、指定私人 email、憑證私鑰或 token 樣式；公開來源與 Release notes 同樣通過檢查。Secret Scanning 為 0 open alerts。
- 2026-09-20：APK 與 AAB 憑證均與 0.2.2 相同，Subject 為 `CN=alzpqm, O=alzpqm`。模擬器已由正式 0.2.2 覆蓋升級至 0.2.3，versionCode 21、minSdk 26、targetSdk 37，冷啟動成功，未見 App crash／ANR。本輪未連接實體手機，不能視為已完成實機測試。
- 2026-09-20：AAB 經 jarsigner 驗章成功；工具另提示自簽憑證、無 timestamp 及串流讀取時 manifest 順序警告。本輪發布目標為 GitHub，未測試 Google Play 上傳。
- 2026-09-20：0.2.3 APK SHA-256：`c877a738846b527908485d0288323cb4b1a0a063584d645fe1043241f96371cc`；AAB：`ac8ba56c835e5672907708427dc159da6cb56236506b45163f35edacca4b79d5`；SHA256SUMS 檔：`e289ac5447a59bea5c5b508e7b0ffe61e841bce58950406c1568b5d98ea1b659`。三個 GitHub assets 的 digest 均與本機一致，正式 Release 非 draft／prerelease，tag 指向 `840c50b`。

- `v0.2.0` 發佈前曾完成建置與 API 37 模擬器、Android 14 實機檢查；本輪 UI 修改後仍須重新驗證，不能沿用舊結果。
- 2026-09-02：最後文件與署名更新後，乾淨執行 `clean testDebugUnitTest lintDebug assembleDebug` 成功，56 個 tasks 全部完成。
- 2026-09-02：API 37 模擬器已驗證輸入、清除、IME Go、鍵盤開合、Stop 與實際追蹤；未發現崩潰或 ANR。實機曾短暫取得授權，但安裝時連線中斷，本輪實機回歸尚待補做。
- 2026-09-02：接手檔初稿曾含本機絕對路徑與裝置序號，但在提交前已移除；後續必須確認完整掃描結果為零阻擋項目。
- 2026-09-02：來源隱私掃描通過，GitHub Secret Scanning 為 0 alerts；既有 0.2.0 APK/AAB 解壓掃描通過，簽章 Subject 僅為 `CN=alzpqm, O=alzpqm`。
- 2026-09-02：工作目錄的同步機制曾產生檔名帶 ` 2` 的 Markdown、Gradle 與 class 副本。舊 Markdown 副本曾含已移除的 email，已在提交前刪除；class 副本曾造成 D8 duplicate class，執行 Gradle `clean` 後恢復。每次提交前都要檢查 `git status --untracked-files=all` 與名稱帶 ` 2` 的檔案。
- 2026-09-02：0.2.1／versionCode 19 已成功安裝到 API 37 模擬器與 Android 14 實機；實機確認套件版本並成功開啟首頁。
- 2026-09-02：0.2.1 Release APK/AAB 與 lintVital 建置成功（59 tasks）；APK manifest 為 application ID `com.surfaceocean.nexttraceroute`、target/compile SDK 37。
- 2026-09-02：0.2.1 產物隱私掃描通過，APK 簽章 Subject 為 `CN=alzpqm, O=alzpqm`。APK SHA-256：`e28a50ce66c56a45edc609fe4ffac556103ec6baecd169907e977f6ceb2a2164`；AAB SHA-256：`c3385873340f025629a81ba7dbfbdd94b8cfebb5f218374646a95b7bbb1693f0`。
- 2026-09-02：遠端 v0.2.1 為正式版與 latest，作者為 `alzpqm`，tag 解析至 `750a517`，兩個 Release assets 的遠端 digest 與上述雜湊一致，GitHub Secret Scanning 為 0 alerts。
- 2026-09-02：`.github/workflows/build.yml` 會在 Release 發佈後重複建置並以 `overwrite: true` 覆蓋已驗證資產。本次重複 run 已取消，遠端資產未被改動；下次發佈前必須修正或移除此流程。
- 2026-09-06：API 37 模擬器完成日／夜模式對照。全新設定下冷啟動可跟隨系統；在深色模式進入設定並按 Save，再切回日間模式及冷啟動，可穩定重現 App 背景仍為黑色、狀態列黑色圖示不可讀。根因位於 `MainActivity` 還原 11 組固定色與 `Settings` 無條件保存這些顏色；本輪僅診斷與記錄，未修改 UI 程式。
- 2026-09-06：0.2.2 修正後完成乾淨 `clean testDebugUnitTest lintDebug assembleDebug`，56 個 tasks 全部成功；lint 為 0 errors、5 個非阻擋更新／目錄整理警告。
- 2026-09-06：API 37 模擬器確認深色模式儲存設定後切回日間模式，首頁、狀態列與導覽列皆正確恢復淺色；重新產生的 `settings.json` 不含任何顏色欄位。
- 2026-09-06：API 37 模擬器完成設定頁頂部、DNS 與進階服務端的深色畫面檢查；大字級下輸入內容不再與標籤重疊，連續 Slider 不再顯示過密刻度。網域解析、IPv4／IPv6 位址選擇、開始追蹤及橫向旋轉皆未出現崩潰或 ANR。
- 2026-09-06：正式簽署工作流程 run `34016502792` 在提交 `92eaca2` 上成功完成 Android 37 安裝、單元測試、lint、APK／AAB 建置、產物隱私掃描與暫存上傳。
- 2026-09-06：0.2.2 正式 APK 可直接以 `adb install -r` 覆蓋 0.2.1；升級後為 versionCode 20、versionName 0.2.2、minSdk 26、targetSdk 37，Activity 冷啟動成功。0.2.1 與 0.2.2 憑證 SHA-256 完全相同，Subject 為 `CN=alzpqm, O=alzpqm`。
- 2026-09-06：0.2.2 發佈產物再次通過本機隱私掃描。APK SHA-256：`8dd786d5adbe407b26af16c6a9bd145cb266293b3d7ad69f1f13405b07d1e054`；AAB SHA-256：`ccb90862f679d2bacc38be0c7b438453058d3175c165b9bf139ee53f0ae58f9e`。
- 2026-09-06：遠端 v0.2.2 為正式版與 latest，作者為 `alzpqm`，tag 解析至 `92eaca2`；APK、AAB 與 SHA256SUMS 三個 Release assets 的遠端 digest 均與本機檔案一致，GitHub Secret Scanning 為 0 alerts。Release 發佈後沒有自動覆蓋資產的工作流程。
