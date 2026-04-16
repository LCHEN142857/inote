// 应用入口导入 React 运行时。
import React from "react";
// 应用入口导入 ReactDOM 客户端渲染能力。
import ReactDOM from "react-dom/client";
// 应用入口导入根组件。
import App from "./App";
// 应用入口导入全局样式。
import "./styles.css";

// 创建根节点并开始渲染整个应用。
ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    {/* 应用根部的 JSX 结构块。 */}
    <App />
  </React.StrictMode>
);
