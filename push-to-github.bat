@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0"

echo ========================================
echo   PvP-Optimize 一键推送到 GitHub
echo ========================================
echo.

REM 1. 当前分支 & 远程
echo [1/6] Git 状态检查
echo ----------------------------------------
for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set BRANCH=%%i
for /f "delims=" %%i in ('git remote get-url origin 2^>nul') do set REMOTE=%%i
echo   分支: %BRANCH%
echo   远程: %REMOTE%
echo.

REM 2. 同步远端 (拉取可能存在的远端提交)
echo [2/6] git fetch + rebase
echo ----------------------------------------
git fetch origin %BRANCH%
if errorlevel 1 (
    echo [错误] git fetch 失败. 检查网络或远程地址
    pause
    exit /b 1
)

REM 探测本地与远端是否分叉
git rev-parse --verify origin/%BRANCH% >nul 2>&1
if errorlevel 1 (
    echo   远端无 %BRANCH% 分支, 跳过 rebase
    goto SKIP_REBASE
)

for /f "delims=" %%i in ('git rev-list --left-right --count origin/%BRANCH%...HEAD') do set AHEAD_BEHIND=%%i
set AHEAD=
set BEHIND=
for /f "tokens=1" %%a in ("%AHEAD_BEHIND%") do set BEHIND=%%a
for /f "tokens=2" %%b in ("%AHEAD_BEHIND%") do set AHEAD=%%b
echo   本地领先: %AHEAD% 个,  落后: %BEHIND% 个

if "%BEHIND%"=="0" goto SKIP_REBASE

echo   远端有新提交, 执行 git pull --rebase
git pull --rebase origin %BRANCH%
if errorlevel 1 (
    echo.
    echo [错误] rebase 失败 —— 可能有冲突.
    echo        手动处理:
    echo            1. 编辑冲突文件
    echo            2. git add .
    echo            3. git rebase --continue
    echo        或放弃 rebase:
    echo            git rebase --abort
    pause
    exit /b 1
)

:SKIP_REBASE
echo.

REM 3. 显示当前变更
echo [3/6] 工作区变更
echo ----------------------------------------
git status --short
echo.

REM 4. 询问 commit message
echo [4/6] 输入提交信息 (回车使用默认)
echo ----------------------------------------
set /p MSG=提交信息:
if "%MSG%"=="" set MSG=update mod %date:~0,10% %time:~0,5%
echo   -^> %MSG%
echo.

REM 5. 暂存并提交
echo [5/6] git add + commit
echo ----------------------------------------
git add -A
if errorlevel 1 (
    echo [错误] git add 失败
    pause
    exit /b 1
)
git commit -m "%MSG%"
if errorlevel 1 (
    echo.
    echo [提示] 没有需要提交的变更, 直接进入 push 阶段
    echo.
)
echo.

REM 6. 推送
echo [6/6] git push origin %BRANCH%
echo ----------------------------------------
git push origin %BRANCH%
if errorlevel 1 (
    echo.
    echo [错误] 推送失败. 请检查上面的 Git 输出.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   推送完成! https://github.com/ccbayxy/pvp-optimize
echo ========================================
echo.
pause
endlocal