import { render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import App from './App.jsx'

function answerCurrentUserWith(response) {
  vi.stubGlobal('fetch', vi.fn(async () => response()))
}

beforeEach(() => {
  answerCurrentUserWith(() => new Response(
    JSON.stringify({ email: 'first.admin@harvester.test', role: 'ADMIN' }),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  ))
  vi.stubGlobal('location', { assign: vi.fn() })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test('shows the sidebar shell with empty Data, Operate and Admin groups', async () => {
  render(<App />)

  const sidebar = screen.getByRole('navigation', { name: 'Sidebar' })
  for (const group of ['Data', 'Operate', 'Admin']) {
    const section = within(sidebar).getByRole('region', { name: group })
    expect(within(section).queryAllByRole('listitem')).toHaveLength(0)
  }
})

test('shows who is signed in', async () => {
  render(<App />)

  expect(await screen.findByText('first.admin@harvester.test')).toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith('/api/me', expect.anything())
})

test('sends a signed-out user to the sign-in page', async () => {
  answerCurrentUserWith(() => new Response(null, { status: 401 }))

  render(<App />)

  await waitFor(() => expect(window.location.assign).toHaveBeenCalledWith('/login'))
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})

test('shows an error when the server answers with an error', async () => {
  answerCurrentUserWith(() => new Response(null, { status: 500 }))

  render(<App />)

  expect(await screen.findByRole('alert')).toHaveTextContent('Could not load your account')
  expect(window.location.assign).not.toHaveBeenCalled()
})

test('shows an error when the server cannot be reached', async () => {
  answerCurrentUserWith(() => { throw new TypeError('Failed to fetch') })

  render(<App />)

  expect(await screen.findByRole('alert')).toHaveTextContent('Could not load your account')
})
