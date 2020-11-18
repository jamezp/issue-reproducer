/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


const getEndpoints = async () => {
    const baseUrl = location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '') + "/web-app/rest";
    const response = await fetch('rest/resteasy/registry');

    response.text().then((xml) => {
        const list = document.getElementById("links");
        const parser = new DOMParser();
        const doc = parser.parseFromString(xml, "application/xml");
        const resources = doc.getElementsByTagName("resource");
        for (let resource of resources) {
            const value = resource.getAttribute("uriTemplate");
            const url = baseUrl + (value.startsWith("/") ? "" : "/") + value;
            const li = document.createElement("li");
            const a = document.createElement("a");
            a.setAttribute("href", url);
            a.setAttribute("target", "_blank");
            a.appendChild(document.createTextNode(url));
            li.appendChild(a);

            const copyElement = createCopyElement();
            copyElement.onclick = function () {
                navigator.clipboard.writeText(url).then(function () {
                    console.log("Success");
                }, function () {
                    console.log("Failed");
                });
            };
            li.appendChild(copyElement);

            list.appendChild(li);
        }
    });
}

function createCopyElement() {
    const span = document.createElement("span");
    span.setAttribute("class", "clipboard");
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", "1em");
    svg.setAttribute("height", "1em");
    svg.setAttribute("viewBox", "0 0 16 16");
    svg.setAttribute("class", "bi bi-clipboard");
    svg.setAttribute("fill", "currentColor");

    const p1 = document.createElementNS("http://www.w3.org/2000/svg", "path");
    p1.setAttribute("fill-rule", "evenodd");
    p1.setAttribute("d", "M4 1.5H3a2 2 0 0 0-2 2V14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V3.5a2 2 0 0 0-2-2h-1v1h1a1 1 0 0 1 1 1V14a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V3.5a1 1 0 0 1 1-1h1v-1z");
    svg.appendChild(p1);

    const tooltip = document.createElementNS("http://www.w3.org/2000/svg", "title");
    tooltip.appendChild(document.createTextNode("Copy to Clipboard"));
    p1.appendChild(tooltip);

    const p2 = document.createElementNS("http://www.w3.org/2000/svg", "path");
    p2.setAttribute("fill-rule", "evenodd");
    p2.setAttribute("d", "M9.5 1h-3a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5zm-3-1A1.5 1.5 0 0 0 5 1.5v1A1.5 1.5 0 0 0 6.5 4h3A1.5 1.5 0 0 0 11 2.5v-1A1.5 1.5 0 0 0 9.5 0h-3z");
    svg.appendChild(p2);

    span.appendChild(svg);
    return span;
}