export default defineAppConfig({
  pages: [
    'pages/login/index',
    'pages/index/index',
    'pages/review/index',
    'pages/targets/index',
    'pages/target-detail/index',
    'pages/notebook/index',
  ],
  window: {
    backgroundTextStyle: 'dark',
    navigationBarBackgroundColor: '#da7756',
    navigationBarTitleText: 'MemoSpark',
    navigationBarTextStyle: 'white',
    backgroundColor: '#f9f8f6',
  },
  // NOTE: tabBar icons are intentionally text-only. WeChat requires every
  // declared iconPath to point to an existing raster (PNG/JPG) file, and we
  // ship no icon assets — declaring missing paths breaks the build. Drop in
  // assets/icons/*.png and re-add iconPath/selectedIconPath when available.
  tabBar: {
    color: '#94a3b8',
    selectedColor: '#da7756',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '今日',
      },
      {
        pagePath: 'pages/review/index',
        text: '复习',
      },
      {
        pagePath: 'pages/targets/index',
        text: '目标',
      },
      {
        pagePath: 'pages/notebook/index',
        text: '笔记',
      },
    ],
  },
})
