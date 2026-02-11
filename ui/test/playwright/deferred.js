import themeCss from "@openremote/theme/default.css";

const style = document.createElement("style");
style.textContent = themeCss;
document.head.appendChild(style);
