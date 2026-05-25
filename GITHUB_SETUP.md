# GitHub 项目设置指南

## 🚀 在 GitHub 上创建项目

### 第一步：在 GitHub 上创建新仓库

1. 访问 https://github.com/new

2. 登录你的 GitHub 账号

3. 点击 "New repository" 按钮

4. 填写仓库信息：
   - **Repository name**: `autonomous-phone-ai`（或你喜欢的名字）`
   - **Description**: `基于本地运行的手机自主AI控制框架`
   - **Public/Private: 选择你喜欢的可见性
   - **不要**勾选 "Initialize this repository"（我们已经有代码了）"

5. 点击 "Create repository"

### 第二步：连接本地仓库到 GitHub

创建完仓库后，GitHub 会显示连接指令。按照以下步骤操作：

#### 方式一：使用 HTTPS（推荐新手）

```bash
cd /workspace/autonomous-phone-framework
git remote add origin https://github.com/你的用户名/autonomous-phone-ai.git
git branch -M main
git push -u origin main
```

#### 方式二：使用 SSH（如果你已经设置了 SSH 密钥）

```bash
cd /workspace/autonomous-phone-framework
git remote add origin git@github.com:你的用户名/autonomous-phone-ai.git
git branch -M main
git push -u origin main
```

**注意**：请将上面的 `你的用户名` 替换为你真实的 GitHub 用户名！

### 第三步：推送到 GitHub

执行上面的命令后，你的代码就会上传到 GitHub！

## 📝 GitHub Actions CI/CD 设置

### 自动构建（可选）

如果你想在 GitHub 上自动构建项目，可以创建 `.github/workflows/build.yml 文件：

```yaml
name: Build Android APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
```

## 🎯 完成后的下一步

1. **GitHub 仓库创建成功后，你可以：
   - 在 GitHub 上管理 Issues 和 Pull Requests
   - 添加项目 Wiki 文档
   - 设置 GitHub Pages（如果需要）
   - 配置 GitHub 仓库设置中的功能

2. **本地开发继续**：
   ```bash
   # 拉取最新代码
   git pull origin main
   
   # 创建新功能分支
   git checkout -b feature/new-feature
   
   # 提交更改
   git add .
   git commit -m "描述你的更改"
   git push origin feature/new-feature
   ```

## 📚 有用的 GitHub 资源

- [GitHub 官方文档](https://docs.github.com)
- [Git 基础教程](https://git-scm.com/docs/gittutorial)
- [GitHub Actions 文档](https://docs.github.com/actions)
