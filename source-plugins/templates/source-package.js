(function () {
  const CONFIG = __MIYU_CONFIG__;

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

  function parseScriptArray(scriptText, variableName) {
    const source = String(scriptText || '');
    const variableIndex = source.indexOf(variableName);
    if (variableIndex === -1) return [];
    const start = source.indexOf('[', variableIndex);
    if (start === -1) return [];
    let depth = 0;
    let quote = '';
    let escaped = false;
    for (let index = start; index < source.length; index += 1) {
      const char = source[index];
      if (quote) {
        if (escaped) {
          escaped = false;
        } else if (char === '\\') {
          escaped = true;
        } else if (char === quote) {
          quote = '';
        }
        continue;
      }
      if (char === '"' || char === "'") {
        quote = char;
        continue;
      }
      if (char === '[') depth += 1;
      if (char === ']') {
        depth -= 1;
        if (depth === 0) {
          try {
            const parsed = JSON.parse(source.slice(start, index + 1));
            return Array.isArray(parsed) ? parsed : [];
          } catch (_error) {
            return [];
          }
        }
      }
    }
    return [];
  }

  function parseScribbleHubItems(doc) {
    const items = Array.from(doc.querySelectorAll('.search_main_box')).map((node) => {
      const link = node.querySelector('.search_title > a[href], a[href*="/series/"]');
      if (!link) return null;
      const path = pathFrom(link.getAttribute('href'));
      const coverNode = node.querySelector('.search_img > img, img');
      const slug = normalizedSlug(path);
      return {
        rawId: slug,
        slug: slug,
        path: path,
        title: cleanText(link.textContent || link.getAttribute('title') || 'Untitled'),
        coverUrl: coverNode ? absoluteUrl(coverNode.getAttribute('data-src') || coverNode.getAttribute('src') || '') : null,
        author: cleanText((node.querySelector('.search_author, .author') || {}).textContent || '') || 'Unknown Author',
        summary: cleanText((node.querySelector('.search_body, .search_synopsis, p') || {}).textContent || ''),
        status: 'Unknown'
      };
    }).filter(Boolean);
    return dedupeByPath(items);
  }

  function scribbleHubPostId(path) {
    const parts = String(path || '').split('/').filter(Boolean);
    if (parts[0] === 'series' && parts[1]) return parts[1];
    return parts[0] || '';
  }

  async function searchScribbleHub(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const url = query
      ? '/?s=' + encodeURIComponent(query) + '&post_type=fictionposts'
      : '/series-finder/?sf=1&sort=ratings&order=desc&pg=' + page;
    const doc = await fetchDocument(url);
    const items = parseScribbleHubItems(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function detailsScribbleHub(payload) {
    const path = payload.path || '/';
    const doc = await fetchDocument(path);
    const postId = scribbleHubPostId(path);
    let chapters = [];
    if (postId) {
      const chapterDoc = await fetchDocument('/wp-admin/admin-ajax.php', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          Referer: absoluteUrl(path),
          Origin: CONFIG.baseUrl
        },
        body: new URLSearchParams({
          action: 'wi_getreleases_pagination',
          pagenum: '-1',
          mypostid: postId
        }).toString()
      });
      chapters = Array.from(chapterDoc.querySelectorAll('.toc_w')).map((node, index) => {
        const link = node.querySelector('a[href]');
        if (!link) return null;
        const chapterPath = pathFrom(link.getAttribute('href'));
        return {
          order: index + 1,
          title: cleanText((node.querySelector('.toc_a') || link).textContent || '') || ('Chapter ' + (index + 1)),
          path: chapterPath,
          updatedAt: cleanText((node.querySelector('.fic_date_pub') || {}).textContent || '') || null
        };
      }).filter(Boolean).reverse().map((chapter, index) => Object.assign({}, chapter, { order: index + 1 }));
    }
    if (!chapters.length) chapters = parseGenericChapterList(doc);
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['.fic_title', 'h1', 'meta[property="og:title"]']) || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['.fic_image > img', 'meta[property="og:image"]', 'img[data-src]', 'img[src]']),
      author: pickFirstText(doc, ['.auth_name_fic', '.author a', '.author']) || payload.fallbackAuthor || 'Unknown Author',
      summary: pickFirstText(doc, ['.wi_fic_desc', '.summary', 'meta[name="description"]']) || payload.fallbackSummary || '',
      status: cleanText((doc.querySelector('.rnd_stats') || {}).nextElementSibling?.textContent || '').indexOf('Ongoing') !== -1 ? 'Ongoing' : 'Completed',
      chapterCount: chapters.length || payload.fallbackChapterCount || null,
      genres: Array.from(doc.querySelectorAll('.fic_genre, a[href*="/genre/"], .genres a')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('.tags a, a[href*="/tag/"]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: chapters
    };
  }

  async function chapterScribbleHub(payload) {
    const chapterPath = payload.path || '/';
    const doc = await fetchDocument(chapterPath);
    const content = doc.querySelector('div.chp_raw, .chp_raw, .chapter-content, article, main') || doc.body;
    return {
      order: parseChapterOrder(chapterPath) || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['.chapter-title', 'h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(cloneContent(content).innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  function parseRoyalRoadItems(doc) {
    const items = Array.from(doc.querySelectorAll('.fiction-list-item')).map((node) => {
      const link = node.querySelector('a[href*="/fiction/"]');
      if (!link) return null;
      const path = pathFrom(link.getAttribute('href'));
      const coverNode = node.querySelector('img.thumbnail, img');
      const title = cleanText(
        (node.querySelector('h2 a, h3 a, .fiction-title a') || {}).textContent ||
        coverNode?.getAttribute('alt') ||
        link.getAttribute('title') ||
        'Untitled'
      );
      const slug = normalizedSlug(path);
      return {
        rawId: slug,
        slug: slug,
        path: path,
        title: title,
        coverUrl: coverNode ? absoluteUrl(coverNode.getAttribute('data-src') || coverNode.getAttribute('src') || '') : null,
        author: cleanText((node.querySelector('a[href*="/profile/"]') || {}).textContent || '') || 'Unknown Author',
        summary: cleanText((node.querySelector('.description, .fiction-description') || {}).textContent || ''),
        status: 'Unknown'
      };
    }).filter(Boolean);
    return dedupeByPath(items);
  }

  async function searchRoyalRoad(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const params = new URLSearchParams({ page: String(page) });
    if (query) {
      params.append('title', query);
      params.append('globalFilters', 'true');
    } else {
      params.append('orderBy', 'last_update');
    }
    const doc = await fetchDocument('/fictions/search?' + params.toString());
    const items = parseRoyalRoadItems(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  function parseRoyalRoadChapters(doc) {
    const scriptText = Array.from(doc.scripts || []).map((script) => script.textContent || '').join('\n');
    const rawChapters = parseScriptArray(scriptText, 'window.chapters');
    const chapters = rawChapters.map((chapter, index) => {
      const chapterPath = pathFrom(chapter.url || chapter.path || '');
      return {
        order: Number(chapter.order || index + 1),
        title: cleanText(chapter.title || '') || ('Chapter ' + (index + 1)),
        path: chapterPath,
        updatedAt: chapter.date || chapter.createdDate || null
      };
    }).filter((chapter) => chapter.path && chapter.path !== '/');
    return dedupeByPath(chapters).sort((left, right) => left.order - right.order);
  }

  async function detailsRoyalRoad(payload) {
    const path = payload.path || '/';
    const doc = await fetchDocument(path);
    const chapters = parseRoyalRoadChapters(doc);
    const statusText = Array.from(doc.querySelectorAll('.label-sm, span[class*=label], .fiction-info span'))
      .map((node) => cleanText(node.textContent))
      .find((text) => /ongoing|hiatus|complete/i.test(text)) || payload.fallbackStatus || 'Unknown';
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['h1', 'meta[property="og:title"]']) || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['img.thumbnail', 'meta[property="og:image"]', 'img[src]']),
      author: pickFirstText(doc, ['a[href*="/profile/"]']) || payload.fallbackAuthor || 'Unknown Author',
      summary: pickFirstText(doc, ['.description', '.fiction-info .description', 'meta[name="description"]']) || payload.fallbackSummary || '',
      status: statusText ? statusText.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : 'Unknown',
      chapterCount: chapters.length || payload.fallbackChapterCount || null,
      rating: Number(pickFirstText(doc, ['[itemprop="ratingValue"]', '.star-container meta[itemprop="ratingValue"]'])) || null,
      genres: Array.from(doc.querySelectorAll('span.tags a, .tags a, a[href*="/fictions/search?tagsAdd"]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('span.tags a, .tags a, a[href*="/fictions/search?tagsAdd"]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: chapters
    };
  }

  async function chapterRoyalRoad(payload) {
    const chapterPath = payload.path || '/';
    const doc = await fetchDocument(chapterPath);
    const content = doc.querySelector('.chapter-content, article, main') || doc.body;
    const copy = cloneContent(content);
    const html = doc.documentElement ? doc.documentElement.innerHTML : '';
    const hiddenClass = (html.match(/<style>[\s\S]*?\.([A-Za-z0-9_-]+)\s*\{[^{}]*display\s*:\s*none/i) || [])[1];
    if (hiddenClass) {
      copy.querySelectorAll('[class]').forEach((node) => {
        if (node.classList && node.classList.contains(hiddenClass)) node.remove();
      });
    }
    return {
      order: parseChapterOrder(chapterPath) || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(copy.innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  function parseNovelFireItems(doc, selector) {
    const items = Array.from(doc.querySelectorAll(selector || '.novel-item')).map((node) => {
      const link = node.querySelector('.novel-title > a[href], a[href]');
      if (!link) return null;
      const path = pathFrom(link.getAttribute('href'));
      const coverNode = node.querySelector('.novel-cover > img, img');
      const slug = normalizedSlug(path);
      return {
        rawId: slug,
        slug: slug,
        path: path,
        title: cleanText(link.textContent || link.getAttribute('title') || 'Untitled'),
        coverUrl: coverNode ? absoluteUrl(coverNode.getAttribute('data-src') || coverNode.getAttribute('src') || '') : null,
        author: 'Unknown Author',
        summary: cleanText((node.querySelector('.summary, .description, p') || {}).textContent || ''),
        status: 'Unknown'
      };
    }).filter(Boolean);
    return dedupeByPath(items);
  }

  async function searchNovelFire(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const url = query
      ? '/search?keyword=' + encodeURIComponent(query) + '&page=' + page
      : '/search-adv?sort=date&page=' + page;
    const doc = await fetchDocument(url);
    const items = parseNovelFireItems(doc, query ? '.novel-list.chapters .novel-item' : '.novel-item');
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function fetchNovelFireChapters(novelPath, doc) {
    const postId = cleanText((doc.querySelector('#novel-report') || {}).getAttribute?.('report-post_id') || '');
    if (!postId) return parseGenericChapterList(doc);
    try {
      const params = new URLSearchParams({
        draw: '1',
        'columns[0][data]': 'n_sort',
        'columns[0][name]': 'cmm_posts_detail.n_sort',
        'columns[0][searchable]': 'true',
        'columns[0][orderable]': 'true',
        'columns[0][search][value]': '',
        'columns[0][search][regex]': 'false',
        'order[0][column]': '0',
        'order[0][dir]': 'asc',
        'order[0][name]': 'cmm_posts_detail.n_sort',
        start: '0',
        length: '-1',
        'search[value]': '',
        'search[regex]': 'false',
        post_id: postId,
        only_bookmark: 'false',
        _: String(Date.now())
      });
      const text = await fetchText('/ajax/listChapterDataAjax?' + params.toString(), {
        headers: {
          accept: 'application/json,text/plain,*/*',
          Referer: absoluteUrl(novelPath)
        }
      });
      const json = JSON.parse(text);
      const rows = Array.isArray(json.data) ? json.data : [];
      return rows.map((entry, index) => {
        const order = Number(entry.n_sort || index + 1);
        if (!Number.isFinite(order)) return null;
        const title = stripHtml(entry.title || entry.slug || '') || ('Chapter ' + order);
        return {
          order: order,
          title: title,
          path: pathFrom(String(novelPath || '').replace(/\/+$/, '') + '/chapter-' + order),
          updatedAt: null
        };
      }).filter(Boolean).sort((left, right) => left.order - right.order);
    } catch (_error) {
      return parseGenericChapterList(doc);
    }
  }

  async function detailsNovelFire(payload) {
    const path = payload.path || '/';
    const doc = await fetchDocument(path);
    const chapters = await fetchNovelFireChapters(path, doc);
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['.novel-title', '.cover > img', 'h1', 'meta[property="og:title"]']) || payload.fallbackTitle || 'Untitled',
      coverUrl: pickFirstAttr(doc, ['.cover > img', 'meta[property="og:image"]', 'img[data-src]', 'img[src]']),
      author: pickFirstText(doc, ['.author .property-item > span', '.author a', '.author']) || payload.fallbackAuthor || 'Unknown Author',
      summary: pickFirstText(doc, ['.summary .content', '.summary', 'meta[name="description"]']) || payload.fallbackSummary || '',
      status: pickFirstText(doc, ['.header-stats .ongoing', '.header-stats .completed', '.status']) || payload.fallbackStatus || 'Unknown',
      chapterCount: chapters.length || parseFirstInt(pickFirstText(doc, ['.header-stats', '.chapter-count'])) || payload.fallbackChapterCount || null,
      rating: Number(pickFirstText(doc, ['.nub', '[itemprop="ratingValue"]'])) || null,
      genres: Array.from(doc.querySelectorAll('.categories .property-item, a[href*="/genre/"], a[href*="/genres/"]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      tags: Array.from(doc.querySelectorAll('.tags a, a[href*="/tag/"], a[href*="/tags/"]')).map((node) => cleanText(node.textContent)).filter(Boolean),
      chapters: chapters
    };
  }

  async function chapterNovelFire(payload) {
    const chapterPath = payload.path || '/';
    const doc = await fetchDocument(chapterPath);
    const content = doc.querySelector('#content, .chapter-content, article, main') || doc.body;
    const copy = cloneContent(content);
    copy.querySelectorAll('*').forEach((node) => {
      const tag = String(node.tagName || '').toLowerCase();
      if (tag.length > 5 && tag.indexOf('nf') === 0) node.remove();
    });
    return {
      order: parseChapterOrder(chapterPath) || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(copy.innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  function parseAo3Items(doc) {
    const items = Array.from(doc.querySelectorAll('li.work')).map((node) => {
      const link = node.querySelector('h4.heading > a[href], h4 a[href], a[href*="/works/"]');
      if (!link) return null;
      const path = pathFrom(link.getAttribute('href'));
      const slug = normalizedSlug(path);
      return {
        rawId: slug,
        slug: slug,
        path: path,
        title: cleanText(link.textContent || 'Untitled'),
        coverUrl: null,
        author: Array.from(node.querySelectorAll('a[rel="author"]')).map((author) => cleanText(author.textContent)).filter(Boolean).join(', ') || 'Unknown Author',
        summary: cleanText((node.querySelector('blockquote.userstuff, .summary') || {}).textContent || ''),
        status: cleanText((node.querySelector('dt.status') || {}).textContent || '') || 'Unknown'
      };
    }).filter(Boolean);
    return dedupeByPath(items);
  }

  async function searchAo3(payload) {
    const page = Math.max(1, Number(payload.page || 1));
    const query = cleanText(payload.query || '');
    const params = new URLSearchParams({ page: String(page) });
    params.append('work_search[language_id]', 'en');
    if (query) {
      params.append('work_search[query]', query);
    } else {
      params.append('work_search[sort_column]', 'revised_at');
      params.append('work_search[sort_direction]', 'desc');
    }
    const doc = await fetchDocument('/works/search?' + params.toString());
    const items = parseAo3Items(doc);
    return { items: items, page: page, hasMore: items.length >= 20 };
  }

  async function fetchAo3Chapters(path, doc) {
    const basePath = String(path || '').replace(/\/+$/, '');
    let chapters = [];
    try {
      const navDoc = await fetchDocument(basePath + '/navigate');
      chapters = Array.from(navDoc.querySelectorAll('ol.index li')).map((node, index) => {
        const link = node.querySelector('a[href]');
        if (!link) return null;
        return {
          order: index + 1,
          title: cleanText(link.textContent || '') || ('Chapter ' + (index + 1)),
          path: pathFrom(link.getAttribute('href')),
          updatedAt: cleanText((node.querySelector('span.datetime') || {}).textContent || '') || null
        };
      }).filter(Boolean);
    } catch (_error) {
      chapters = [];
    }
    if (chapters.length) return dedupeByPath(chapters);
    chapters = Array.from(doc.querySelectorAll('#chapter_index select option')).map((option, index) => {
      const code = cleanText(option.getAttribute('value') || '');
      if (!code) return null;
      return {
        order: index + 1,
        title: cleanText(option.textContent || '') || ('Chapter ' + (index + 1)),
        path: pathFrom(basePath + '/chapters/' + code),
        updatedAt: null
      };
    }).filter(Boolean);
    if (chapters.length) return dedupeByPath(chapters);
    chapters = Array.from(doc.querySelectorAll('#chapters h3.title a[href]')).map((link, index) => ({
      order: index + 1,
      title: cleanText(link.parentElement?.textContent || link.textContent || '') || ('Chapter ' + (index + 1)),
      path: pathFrom(link.getAttribute('href')),
      updatedAt: null
    }));
    if (chapters.length) return dedupeByPath(chapters);
    return [{ order: 1, title: pickFirstText(doc, ['h2.title', 'h1', 'h2']) || 'Chapter 1', path: pathFrom(path), updatedAt: null }];
  }

  async function detailsAo3(payload) {
    const path = payload.path || '/';
    const doc = await fetchDocument(path);
    const chapters = await fetchAo3Chapters(path, doc);
    const fandom = Array.from(doc.querySelectorAll('dd.fandom.tags li a.tag')).map((node) => cleanText(node.textContent)).filter(Boolean);
    const tags = Array.from(doc.querySelectorAll('dd.freeform.tags li a.tag, dd.relationship.tags li a.tag, dd.character.tags li a.tag')).map((node) => cleanText(node.textContent)).filter(Boolean);
    const warnings = Array.from(doc.querySelectorAll('dd.warning.tags li a.tag')).map((node) => cleanText(node.textContent)).filter(Boolean);
    const summary = pickFirstText(doc, ['blockquote.userstuff', '.summary']) || payload.fallbackSummary || '';
    return {
      rawId: String(payload.rawId || normalizedSlug(path)),
      slug: String(payload.slug || normalizedSlug(path)),
      path: path,
      title: pickFirstText(doc, ['h2.title', 'h1', 'meta[property="og:title"]']) || payload.fallbackTitle || 'Untitled',
      coverUrl: null,
      author: Array.from(doc.querySelectorAll('a[rel="author"]')).map((node) => cleanText(node.textContent)).filter(Boolean).join(', ') || payload.fallbackAuthor || 'Unknown Author',
      summary: summary,
      status: pickFirstText(doc, ['dt.status']).indexOf('Updated') !== -1 ? 'Ongoing' : 'Completed',
      chapterCount: chapters.length || payload.fallbackChapterCount || null,
      genres: fandom,
      tags: tags.concat(warnings),
      chapters: chapters
    };
  }

  async function chapterAo3(payload) {
    const chapterPath = payload.path || '/';
    const doc = await fetchDocument(chapterPath);
    doc.querySelectorAll('h3.title a').forEach((link) => link.removeAttribute('href'));
    doc.querySelectorAll('h3.landmark.heading#work').forEach((node) => node.remove());
    const content = doc.querySelector('div#chapters > div, #chapters, .userstuff, article, main') || doc.body;
    return {
      order: parseChapterOrder(chapterPath) || Number(payload.chapterNo || 1),
      title: pickFirstText(doc, ['h3.title', 'h2.title', 'h1', 'h2']) || payload.chapterTitle || ('Chapter ' + (payload.chapterNo || 1)),
      html: sanitizeHtml(cloneContent(content).innerHTML || paragraphHtmlFromText(content.textContent || ''))
    };
  }

  async function searchPayload(payload) {
    if (CONFIG.mode === 'readwn') return searchReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return searchFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return searchNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return searchLightNovelPub(payload);
    if (CONFIG.mode === 'scribblehub') return searchScribbleHub(payload);
    if (CONFIG.mode === 'royalroad') return searchRoyalRoad(payload);
    if (CONFIG.mode === 'novelfire') return searchNovelFire(payload);
    if (CONFIG.mode === 'ao3') return searchAo3(payload);
    throw new Error('This package expects a dedicated runtime source file.');
  }

  async function detailsPayload(payload) {
    if (CONFIG.mode === 'readwn') return detailsReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return detailsFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return detailsNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return detailsLightNovelPub(payload);
    if (CONFIG.mode === 'scribblehub') return detailsScribbleHub(payload);
    if (CONFIG.mode === 'royalroad') return detailsRoyalRoad(payload);
    if (CONFIG.mode === 'novelfire') return detailsNovelFire(payload);
    if (CONFIG.mode === 'ao3') return detailsAo3(payload);
    throw new Error('This package expects a dedicated runtime source file.');
  }

  async function chapterPayload(payload) {
    if (CONFIG.mode === 'readwn') return chapterReadwn(payload);
    if (CONFIG.mode === 'freewebnovel') return chapterFreeWebNovel(payload);
    if (CONFIG.mode === 'novelcool') return chapterNovelCool(payload);
    if (CONFIG.mode === 'lightnovelpub') return chapterLightNovelPub(payload);
    if (CONFIG.mode === 'scribblehub') return chapterScribbleHub(payload);
    if (CONFIG.mode === 'royalroad') return chapterRoyalRoad(payload);
    if (CONFIG.mode === 'novelfire') return chapterNovelFire(payload);
    if (CONFIG.mode === 'ao3') return chapterAo3(payload);
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
