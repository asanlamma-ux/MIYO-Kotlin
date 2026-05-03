(function () {
  const CONFIG = {
  "mode": "readwn",
  "bridgeScope": "wuxiaspot",
  "providerLabel": "WuxiaSpot",
  "providerId": "WUXIASPOT",
  "baseUrl": "https://www.wuxiaspot.com",
  "startUrl": "https://www.wuxiaspot.com/list/all/all-newstime-0.html"
};

  if (window.__MIYO_SOURCE_BRIDGE_READY) {
    window.__MIYO_SOURCE_BRIDGE.postReady();
    return true;
  }

  function post(message) {
    if (!window.AndroidExternalSourceBridge) return;
    window.AndroidExternalSourceBridge.postMessage(JSON.stringify(Object.assign({
      scope: CONFIG.bridgeScope,
      providerId: CONFIG.bridgeScope
    }, message)));
  }

  function postReady() {
    post({ type: 'ready' });
  }

  function sendResult(id, payload) {
    post({ type: 'result', id: id, payload: payload });
  }

  function sendError(id, error) {
    const message = error && error.message ? error.message : String(error || 'Plugin request failed.');
    post({ type: 'error', id: id, error: message });
  }

  function sendProgress(id, completed, total) {
    post({
      type: 'progress',
      id: id,
      payload: { completed: completed, total: total }
    });
  }

  function challengeError(message) {
    const error = new Error(message || 'Verification is required before this source can run.');
    error.__challenge = true;
    return error;
  }

  function cleanText(value) {
    return String(value || '').replace(/\s+/g, ' ').trim();
  }

  function stripHtml(html) {
    if (!html) return '';
    const doc = new DOMParser().parseFromString(String(html), 'text/html');
    return cleanText(doc.body ? doc.body.textContent : '');
  }

  function sanitizeHtml(html) {
    return String(html || '')
      .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
      .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
      .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
      .replace(/<ins\b[^>]*>[\s\S]*?<\/ins>/gi, '')
      .replace(/\son[a-z]+\s*=\s*"[^"]*"/gi, '')
      .replace(/\son[a-z]+\s*=\s*'[^']*'/gi, '')
      .replace(/\son[a-z]+\s*=\s*[^\s>]+/gi, '');
  }

  function escapeHtml(value) {
    return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function paragraphHtmlFromText(text) {
    return String(text || '')
      .split(/\n{2,}/)
      .map((paragraph) => cleanText(paragraph))
      .filter(Boolean)
      .map((paragraph) => '<p>' + escapeHtml(paragraph) + '</p>')
      .join('') || '<p><em>No chapter content was returned.</em></p>';
  }

  function absoluteUrl(value) {
    return new URL(String(value || ''), CONFIG.baseUrl).toString();
  }

  function pathFrom(value) {
    if (!value) return '/';
    const url = new URL(String(value), CONFIG.baseUrl);
    return url.pathname + (url.search || '');
  }

  function slugFromPath(path) {
    return String(path || '')
      .substringBefore ? String(path || '').substringBefore('?') : String(path || '').split('?')[0];
  }

  function normalizedSlug(path) {
    const cleanPath = String(path || '').split('?')[0];
    return cleanPath
      .replace(/\/+$/, '')
      .split('/')
      .filter(Boolean)
      .pop()
      ?.replace(/\.html?$/i, '')
      ?.replace(/^chapter-\d+$/i, '')
      || 'novel';
  }

  function pickFirstText(root, selectors) {
    for (let index = 0; index < selectors.length; index += 1) {
      const node = root.querySelector(selectors[index]);
      if (!node) continue;
      const value = cleanText(node.getAttribute('content') || node.textContent || '');
      if (value) return value;
    }
    return '';
  }

  function pickFirstAttr(root, selectors, attrs) {
    const names = attrs || ['content', 'data-src', 'src', 'href'];
    for (let index = 0; index < selectors.length; index += 1) {
      const node = root.querySelector(selectors[index]);
      if (!node) continue;
      for (let attrIndex = 0; attrIndex < names.length; attrIndex += 1) {
        const value = cleanText(node.getAttribute(names[attrIndex]) || '');
        if (value) return absoluteUrl(value);
      }
    }
    return null;
  }

  function parseFirstInt(value) {
    const match = String(value || '').replace(/,/g, '').match(/(\d+)/);
    return match ? Number(match[1]) : null;
  }

  function isChallengeHtml(html, title) {
    const sample = cleanText((title || '') + ' ' + stripHtml(html)).toLowerCase();
    return sample.indexOf('just a moment') !== -1 ||
      sample.indexOf('security verification') !== -1 ||
      sample.indexOf('please complete the turnstile challenge') !== -1 ||
      sample.indexOf('captcha') !== -1 ||
      sample.indexOf('verify you are human') !== -1 ||
      sample.indexOf('checking if the site connection is secure') !== -1;
  }

  async function fetchText(url, init) {
    const response = await fetch(absoluteUrl(url), Object.assign({
      credentials: 'include',
      headers: {
        accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
      }
    }, init || {}));
    const text = await response.text();
    if (isChallengeHtml(text, response.url)) {
      throw challengeError('Please complete the site verification and retry.');
    }
    if (!response.ok) {
      throw new Error('Request failed (' + response.status + ')');
    }
    return text;
  }

  async function fetchDocument(url, init) {
    const text = await fetchText(url, init);
    return new DOMParser().parseFromString(text, 'text/html');
  }

  function parseChapterOrder(path) {
    const cleanPath = String(path || '').split('?')[0].split('#')[0];
    const patterns = [
      /(?:chapter[-_/]|_)(\d+)(?:\.html?)?\/?$/i,
      /\/(\d+)(?:\.html?)?\/?$/i
    ];
    for (let index = 0; index < patterns.length; index += 1) {
      const match = cleanPath.match(patterns[index]);
      if (match) {
        const value = Number(match[1]);
        if (!Number.isNaN(value)) return value;
      }
    }
    return null;
  }

  function likelyChapter(path, title) {
    const cleanPath = String(path || '').toLowerCase();
    const cleanTitle = cleanText(title || '').toLowerCase();
    if (!cleanPath || cleanPath === '/' || cleanPath.indexOf('/search') !== -1 || cleanPath.indexOf('/genre') !== -1 || cleanPath.indexOf('/tag') !== -1) {
      return false;
    }
    return cleanPath.indexOf('chapter') !== -1 ||
      cleanPath.indexOf('chap') !== -1 ||
      parseChapterOrder(path) != null ||
      cleanTitle.indexOf('chapter') === 0 ||
      cleanTitle.indexOf('prologue') === 0 ||
      cleanTitle.indexOf('epilogue') === 0;
  }

  function dedupeByPath(items) {
    const seen = new Set();
    return items.filter((item) => {
      const key = String(item.path || '');
      if (!key || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  function cloneContent(node) {
    const copy = node.cloneNode(true);
    copy.querySelectorAll('script,style,iframe,ins,nav,header,footer,.ads,.ad,.read-ads,.paging,.comments,.catalog').forEach((entry) => entry.remove());
    return copy;
  }

  function genericSelectorSets() {
    return [
      { container: '.book-list .book-item', link: 'a[href]', title: '.book-name, .bookdetail-booktitle, h3, h4', cover: '.book-pic img, img', stats: '.chapter, .book-data-item, .book-data', status: '.status, .book-type', summary: '.book-intro, .book-summary-content' },
      { container: '.ul-list1 .li-row .li .con, .ul-list1-2 .li-row .li .con', link: '.txt h3 a[href], .pic a[href], a[href]', title: '.txt .tit, .txt h3, .tit', cover: '.pic img, img', stats: '.chapter, .chapter .s1', status: '.chapter .s2, .status', summary: '.desc, p' },
      { container: 'ul.novel-list li.novel-item', link: 'a[href]', title: '.novel-title, h4', cover: 'img', stats: '.novel-stats', status: '.status', summary: 'p' },
      { container: '.book-list .bookinfo, .m-book-list .bookinfo', link: 'a[href]', title: '.bookdetail-booktitle, .book-intro-title, h3, h4', cover: 'img', stats: null, status: null, summary: '.intro' },
      { container: '.m-book-item, .col-novel .novel-item, .col-content .novel-item, .col-content .m-book-item', link: 'a.tit[href], a[href]', title: '.tit, .name, h3, h4', cover: 'img', stats: '.chapter', status: '.con', summary: '.intro' },
      { container: '.list-of-novels .col-novel, .list-of-novels .novel', link: 'a[href]', title: '.novel-title, h3, h4', cover: 'img', stats: '.total-chapters', status: '.status', summary: '.description' },
      { container: '.col-12.col-sm-6, .book-item', link: 'a[href]', title: 'h3, h4, .title', cover: 'img', stats: null, status: null, summary: 'p' }
    ];
  }

  function parseGenericNovelList(doc) {
    const selectorSets = genericSelectorSets();
    for (let setIndex = 0; setIndex < selectorSets.length; setIndex += 1) {
      const selectors = selectorSets[setIndex];
      const items = Array.from(doc.querySelectorAll(selectors.container)).map((node) => {
        const link = node.querySelector(selectors.link) || node.closest('a[href]');
        if (!link) return null;
        const path = pathFrom(link.getAttribute('href'));
        if (!path || path === '/') return null;
        const titleNode = selectors.title ? node.querySelector(selectors.title) : link;
        const coverNode = selectors.cover ? node.querySelector(selectors.cover) : null;
        const statsNode = selectors.stats ? node.querySelector(selectors.stats) : null;
        const statusNode = selectors.status ? node.querySelector(selectors.status) : null;
        const summaryNode = selectors.summary ? node.querySelector(selectors.summary) : null;
        const slug = normalizedSlug(path);
        return {
          rawId: slug,
          slug: slug,
          path: path,
          title: cleanText((titleNode && titleNode.textContent) || link.getAttribute('title') || 'Untitled'),
          coverUrl: coverNode ? absoluteUrl(coverNode.getAttribute('data-src') || coverNode.getAttribute('src') || '') : null,
          author: 'Unknown Author',
          summary: cleanText(summaryNode ? summaryNode.textContent : ''),
          status: cleanText(statusNode ? statusNode.textContent : '') || 'Unknown',
          chapterCount: statsNode ? parseFirstInt(statsNode.textContent) : null
        };
      }).filter(Boolean);
      if (items.length) return dedupeByPath(items);
    }
    return [];
  }

  function parseGenericChapterList(doc) {
    const selectors = [
      'ul.chapter-list li a[href]',
      '.chapter-list a[href]',
      '.chapterlist a[href]',
      '.chapter-list .chapter-title a[href]',
      'ul.list-chapter li a[href]',
      '.list-chapter a[href]',
      '.list-chapters a[href]',
      '#chapter-list a[href]',
      '.chapters-list a[href]',
      '.episode-list a[href]',
      '#chp-catalogue-m a[href]',
      '.chapter-item a[href]',
      '.m-newest2 ul li a[href]',
      '.catalog a[href*=chapter]',
      '[class*=chapter] a[href]',
      'li a[href*=chapter]',
      'a[href*=/chapter/]'
    ];
    for (let selectorIndex = 0; selectorIndex < selectors.length; selectorIndex += 1) {
      const selector = selectors[selectorIndex];
      const chapters = Array.from(doc.querySelectorAll(selector)).map((link, index) => {
        const path = pathFrom(link.getAttribute('href'));
        const order = parseChapterOrder(path) || (index + 1);
        const title = cleanText(link.textContent || '') || ('Chapter ' + order);
        return { order: order, title: title, path: path };
      }).filter((chapter) => chapter.path && likelyChapter(chapter.path, chapter.title));
      if (chapters.length) {
        return dedupeByPath(chapters).sort((left, right) => left.order - right.order);
      }
    }
    return [];
  }

  function parseGenericDetails(doc, payload, chapters) {
    const path = payload.path || '/';
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['.r-n-bk-name', 'h1.booktitle', 'h1.tit', 'h1.novel-title', '.novel-title h1', 'h1.title', 'h1', 'meta[property="og:title"]', 'meta[name="twitter:title"]']) || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['meta[property="og:image"]', '.cover img', '.bookdetailimg img', '.pic img', '.book-cover img', 'img.cover', 'img[data-src]', 'img[src]']),
      author: pickFirstText(doc, ['[itemprop="author"]', '.author a', '.author span:last-child', '.author', '.book-author a', '.novel-author', 'a[href*="/author/"]', 'meta[name="author"]']) || payload.fallbackAuthor || 'Unknown Author',
      summary: pickFirstText(doc, ['[itemprop="description"]', '.intro-content', '.book-intro', '.summary .content', '.summary', '.description', '.synops', '.synopsis', '.bk-summary-txt', '.book-summary-content', 'meta[property="og:description"]', 'meta[name="description"]']) || payload.fallbackSummary || '',
      status: pickFirstText(doc, ['.header-stats strong:last-child', '.status', '[class*=novel-status]', '[class*=book-status]', '.chapter .s2']) || payload.fallbackStatus || 'Unknown',
      chapterCount: parseFirstInt(pickFirstText(doc, ['.header-stats strong', '.chapter-count', '.book-chapter', '.book-stats'])) || payload.fallbackChapterCount || chapters.length || null,
      genres: Array.from(doc.querySelectorAll('.categories .property-item, .genres a, .genre a, a[href*=genre], a[href*=category], a[href*=genres]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('.categories .tag, .tags .content .tag, .tags a, .tag a, a[href*=tag], a[href*=tags]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: chapters
    };
  }

  function parseGenericChapterDocument(doc, chapterPath, fallbackOrder, fallbackTitle) {
    const selectors = ['.chapter-reading-section-list', '.txt #article', '#article', '.m-read .txt', '.chapter-content', '#chapter-content', '.reading-content', '.text-left', '.content-text', '.chapter-text', '#content', '#chapterContent', '.novel-content', '.entry-content', 'article', 'main'];
    let contentNode = null;
    for (let index = 0; index < selectors.length; index += 1) {
      const node = doc.querySelector(selectors[index]);
      if (node) {
        contentNode = node;
        break;
      }
    }
    if (!contentNode) contentNode = doc.body;
    const title = pickFirstText(doc, ['h1', 'h2.chapter-title', 'h2']) || fallbackTitle || ('Chapter ' + fallbackOrder);
    return {
      order: parseChapterOrder(chapterPath) || fallbackOrder,
      title: title,
      html: sanitizeHtml(cloneContent(contentNode).innerHTML || paragraphHtmlFromText(contentNode.textContent || ''))
    };
  }

  function parseReadwnNovelItem(node) {
    const link = node.querySelector('a[href]');
    if (!link) return null;
    const path = pathFrom(link.getAttribute('href'));
    const slug = normalizedSlug(path);
    const image = node.querySelector('.novel-cover img, img');
    return {
      rawId: slug,
      slug: slug,
      path: path,
      title: cleanText((node.querySelector('h4, .novel-title') || link).textContent || link.getAttribute('title') || 'Untitled'),
      coverUrl: image ? absoluteUrl(image.getAttribute('data-src') || image.getAttribute('src') || '') : null,
      author: 'Unknown Author',
      summary: cleanText((node.querySelector('p') || {}).textContent || ''),
      status: cleanText((node.querySelector('.status') || {}).textContent || '') || 'Unknown',
      chapterCount: parseFirstInt((node.querySelector('.novel-stats') || {}).textContent || '')
    };
  }

  function parseReadwnChapters(doc, novelPath) {
    const nodes = Array.from(doc.querySelectorAll('.chapter-list li a[href], .chapter-list a[href]'));
    const chapters = nodes.map((link, index) => {
      const path = pathFrom(link.getAttribute('href'));
      const order = parseFirstInt((link.querySelector('.chapter-no') || {}).textContent || '') || parseChapterOrder(path) || (index + 1);
      const title = cleanText((link.querySelector('.chapter-title') || {}).textContent || link.getAttribute('title') || link.textContent || '') || ('Chapter ' + order);
      const updatedAt = cleanText((link.querySelector('time.chapter-update, .chapter-update, time') || {}).textContent || '');
      return {
        order: order,
        title: title,
        path: path,
        updatedAt: updatedAt || null
      };
    }).filter((chapter) => chapter.path && likelyChapter(chapter.path, chapter.title));
    const latestChapterNo = parseFirstInt(pickFirstText(doc, ['.header-stats span strong', '.header-stats strong']));
    const cleanNovelPath = String(novelPath || '');
    if (latestChapterNo && cleanNovelPath.indexOf('.html') !== -1 && latestChapterNo > chapters.length) {
      const lastKnown = chapters.reduce((max, chapter) => Math.max(max, chapter.order || 0), 0);
      for (let order = lastKnown + 1; order <= latestChapterNo; order += 1) {
        chapters.push({
          order: order,
          title: 'Chapter ' + order,
          path: cleanNovelPath.replace(/\.html?$/i, '_' + order + '.html'),
          updatedAt: null
        });
      }
    }
    return dedupeByPath(chapters).sort((left, right) => left.order - right.order);
  }

  async function searchReadwn(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const doc = query
      ? await fetchDocument('/e/search/index.php', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            Referer: absoluteUrl('/search.html'),
            Origin: CONFIG.baseUrl
          },
          body: new URLSearchParams({
            show: 'title',
            tempid: '1',
            tbname: 'news',
            keyboard: query
          }).toString()
        })
      : await fetchDocument('/list/all/all-newstime-' + Math.max(0, page - 1) + '.html');
    const items = Array.from(doc.querySelectorAll('li.novel-item')).map(parseReadwnNovelItem).filter(Boolean);
    return { items: dedupeByPath(items), page: page, hasMore: items.length >= 20 };
  }

  async function detailsReadwn(payload) {
    const path = payload.path || ('/novel/' + (payload.slug || 'novel') + '.html');
    const doc = await fetchDocument(path);
    const chapters = parseReadwnChapters(doc, path);
    const statusText = Array.from(doc.querySelectorAll('div.header-stats > span')).map((span) => {
      const label = cleanText((span.querySelector('small') || {}).textContent || '');
      if (label.toLowerCase() === 'status') {
        return cleanText((span.querySelector('strong') || {}).textContent || '');
      }
      return '';
    }).find(Boolean) || payload.fallbackStatus || 'Unknown';
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['h1.novel-title', 'h1', '.novel-title']) || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['figure.cover img', '.novel-cover img', '.cover img', 'img[data-src]', 'img[src]']),
      author: pickFirstText(doc, ['span[itemprop="author"]', '.author a', '.author span:last-child', '.author']) || payload.fallbackAuthor || 'Unknown Author',
      summary: cleanText((doc.querySelector('.summary') || {}).textContent || '') || payload.fallbackSummary || '',
      status: statusText,
      chapterCount: chapters.length || payload.fallbackChapterCount || null,
      genres: Array.from(doc.querySelectorAll('div.categories > ul > li, .categories li')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('.tags a, .tag a')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: chapters
    };
  }

  async function chapterReadwn(payload) {
    const chapterPath = payload.path || ('/chapter-' + (payload.chapterNo || 1) + '.html');
    const doc = await fetchDocument(chapterPath);
    const content = doc.querySelector('.chapter-content, #chapter-content, .reading-content, article, main') || doc.body;
    return {
      order: parseChapterOrder(chapterPath) || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['#chapter-article .titles h2', 'h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(cloneContent(content).innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  async function searchFreeWebNovel(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const doc = query
      ? await fetchDocument('/search', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ searchkey: query }).toString()
        })
      : await fetchDocument('/novel-list/?page=' + page);
    const items = parseGenericNovelList(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function detailsFreeWebNovel(payload) {
    const path = payload.path || '/';
    const doc = await fetchDocument(path);
    const chapters = dedupeByPath(Array.from(doc.querySelectorAll('#idData a[href], .chapter-list a[href], .list-chapter a[href]')).map((link, index) => {
      const chapterPath = pathFrom(link.getAttribute('href'));
      const order = parseChapterOrder(chapterPath) || (index + 1);
      return {
        order: order,
        title: cleanText(link.getAttribute('title') || link.textContent || '') || ('Chapter ' + order),
        path: chapterPath,
        updatedAt: null
      };
    })).filter((chapter) => chapter.path && likelyChapter(chapter.path, chapter.title)).sort((left, right) => left.order - right.order);
    return parseGenericDetails(doc, payload, chapters);
  }

  async function chapterFreeWebNovel(payload) {
    const doc = await fetchDocument(payload.path || '/');
    return parseGenericChapterDocument(doc, payload.path || '/', Number(payload.chapterNo || 1), payload.chapterTitle || '');
  }

  async function searchNovelCool(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const url = query ? '/search/?name=' + encodeURIComponent(query) + '&page=' + page : '/category/page-' + page + '/';
    const doc = await fetchDocument(url);
    const items = parseGenericNovelList(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function detailsNovelCool(payload) {
    const doc = await fetchDocument(payload.path || '/');
    const chapters = parseGenericChapterList(doc);
    return parseGenericDetails(doc, payload, chapters);
  }

  async function chapterNovelCool(payload) {
    const doc = await fetchDocument(payload.path || '/');
    return parseGenericChapterDocument(doc, payload.path || '/', Number(payload.chapterNo || 1), payload.chapterTitle || '');
  }

  async function searchLightNovelPub(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const url = query ? '/search?title=' + encodeURIComponent(query) + '&page=' + page : '/browse/?page=' + page;
    const doc = await fetchDocument(url);
    const items = parseGenericNovelList(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function fetchLightNovelPubChapters(path) {
    const firstDoc = await fetchDocument(path);
    const title = pickFirstText(firstDoc, ['h1.novel-title', 'h1']);
    const chapterTotal = parseFirstInt(pickFirstText(firstDoc, ['.header-stats span:first-child strong', '.header-stats strong']));
    const totalPages = Math.max(1, Math.ceil((chapterTotal || 0) / 100));
    const chapters = [];
    for (let page = 1; page <= totalPages; page += 1) {
      const doc = page === 1 ? firstDoc : await fetchDocument(path.replace(/\/+$/, '') + '/chapters/page-' + page);
      Array.from(doc.querySelectorAll('.chapter-list li a[href], .chapter-list a[href]')).forEach((link, index) => {
        const chapterPath = pathFrom(link.getAttribute('href'));
        const chapterNo = cleanText((link.querySelector('.chapter-no') || {}).textContent || '');
        const titleText = cleanText((link.querySelector('.chapter-title') || {}).textContent || '');
        const order = parseFirstInt(chapterNo) || parseChapterOrder(chapterPath) || ((page - 1) * 100 + index + 1);
        chapters.push({
          order: order,
          title: cleanText(('Chapter ' + chapterNo + ' - ' + titleText).replace(/-\s*$/g, '')) || ('Chapter ' + order),
          path: chapterPath,
          updatedAt: cleanText((link.querySelector('.chapter-update') || {}).getAttribute?.('datetime') || (link.querySelector('.chapter-update') || {}).textContent || '') || null
        });
      });
    }
    return {
      detailsDoc: firstDoc,
      title: title,
      chapters: dedupeByPath(chapters).filter((chapter) => chapter.path).sort((left, right) => left.order - right.order)
    };
  }

  async function detailsLightNovelPub(payload) {
    const path = payload.path || '/';
    const packageData = await fetchLightNovelPubChapters(path);
    const doc = packageData.detailsDoc;
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: packageData.title || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['figure.cover img', '.cover img', 'img[data-src]', 'img[src]']),
      author: pickFirstText(doc, ['.author > a > span', '.author a', '.author', '[itemprop="author"]']) || payload.fallbackAuthor || 'Unknown Author',
      summary: pickFirstText(doc, ['.summary > .content', '.summary', '[itemprop="description"]', 'meta[name="description"]']) || payload.fallbackSummary || '',
      status: pickFirstText(doc, ['.header-stats span:last-child strong', '.status']) || payload.fallbackStatus || 'Unknown',
      chapterCount: packageData.chapters.length || parseFirstInt(pickFirstText(doc, ['.header-stats span:first-child strong', '.header-stats strong'])) || null,
      genres: Array.from(doc.querySelectorAll('.categories ul li, a[href*=genre], a[href*=category]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('.tags a, a[href*=tag]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: packageData.chapters
    };
  }

  async function chapterLightNovelPub(payload) {
    const doc = await fetchDocument(payload.path || '/');
    const content = doc.querySelector('#chapter-container, .chapter-container, .chapter-content, article, main') || doc.body;
    return {
      order: parseChapterOrder(payload.path || '') || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(cloneContent(content).innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  async function searchPayload(payload) {
    if (CONFIG.mode === 'readwn') return searchReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return searchFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return searchNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return searchLightNovelPub(payload);
    throw new Error('This package expects a dedicated runtime source file.');
  }

  async function detailsPayload(payload) {
    if (CONFIG.mode === 'readwn') return detailsReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return detailsFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return detailsNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return detailsLightNovelPub(payload);
    throw new Error('This package expects a dedicated runtime source file.');
  }

  async function chapterPayload(payload) {
    if (CONFIG.mode === 'readwn') return chapterReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return chapterFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return chapterNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return chapterLightNovelPub(payload);
    throw new Error('This package expects a dedicated runtime source file.');
  }

  async function chaptersPayload(requestId, payload) {
    const chapters = Array.isArray(payload.chapters) ? payload.chapters.slice() : [];
    const results = [];
    const failures = [];
    for (let index = 0; index < chapters.length; index += 1) {
      const chapter = chapters[index];
      try {
        const result = await chapterPayload({
          path: chapter.path,
          chapterNo: chapter.order,
          chapterTitle: chapter.title
        });
        results.push(result);
      } catch (error) {
        failures.push('Chapter ' + (chapter.order || (index + 1)) + ': ' + (error && error.message ? error.message : String(error)));
      } finally {
        sendProgress(requestId, index + 1, chapters.length);
      }
    }
    return {
      chapters: results.sort((left, right) => left.order - right.order),
      failures: failures
    };
  }

  const bridge = {
    postReady: postReady,
    run: async function (request) {
      const id = request && request.id ? String(request.id) : '';
      const type = request && request.type ? String(request.type) : '';
      const payload = request && request.payload ? request.payload : {};
      try {
        if (type === 'search') {
          sendResult(id, await searchPayload(payload));
          return;
        }
        if (type === 'details') {
          sendResult(id, await detailsPayload(payload));
          return;
        }
        if (type === 'chapter') {
          sendResult(id, await chapterPayload(payload));
          return;
        }
        if (type === 'chapters') {
          sendResult(id, await chaptersPayload(id, payload));
          return;
        }
        throw new Error('Unknown request type: ' + type);
      } catch (error) {
        if (error && error.__challenge) {
          post({ type: 'challenge' });
          return;
        }
        sendError(id, error);
      }
    }
  };

  window.__MIYO_SOURCE_BRIDGE = bridge;
  window.__MIYO_SOURCE_BRIDGE_READY = true;

  if (isChallengeHtml(document.documentElement ? document.documentElement.outerHTML : '', document.title || '')) {
    post({ type: 'challenge' });
    return true;
  }

  postReady();
  return true;
})();
