import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'
import ManagerEvents from './ManagerEvents'
import { getManagerEvents, makeDecision } from '../../api/managerApi'

vi.mock('../../api/managerApi', () => ({
  getManagerEvents: vi.fn(),
  getPendingEvents: vi.fn(),
  makeDecision: vi.fn()
}))

describe('ManagerEvents', () => {
  const mockEvents = [
    {
      id: 1,
      title: 'Pending Grant',
      description: 'You received resources.',
      status: 'PENDING',
      options: [
        { code: 'ALLOCATE_SCOUTING', title: 'Allocate Scouting', consequenceDescription: 'Gain scouting resources' }
      ]
    },
    {
      id: 2,
      title: 'Past Event',
      description: 'Already resolved.',
      status: 'RESOLVED',
      resolutionText: 'Resources acquired.'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('renders loading state initially without initialEvents', () => {
    getManagerEvents.mockResolvedValue({ data: [] })
    render(<ManagerEvents />)
    expect(screen.getByText(/loading your events/i)).toBeInTheDocument()
  })

  test('renders empty state if no events', async () => {
    getManagerEvents.mockResolvedValue({ data: [] })
    render(<ManagerEvents />)
    await waitFor(() => {
      expect(screen.getByText(/No Events/i)).toBeInTheDocument()
    })
  })

  test('renders pending and past events correctly', () => {
    render(<ManagerEvents initialEvents={mockEvents} />)
    expect(screen.getByText('Pending Grant')).toBeInTheDocument()
    expect(screen.getByText('Past Event')).toBeInTheDocument()
    expect(screen.getByText('Pending Decisions')).toBeInTheDocument()
    expect(screen.getByText('Past Events')).toBeInTheDocument()
  })

  test('submits decision and updates state', async () => {
    makeDecision.mockResolvedValue({
      data: {
        ...mockEvents[0],
        status: 'RESOLVED',
        resolutionText: 'Successfully allocated scouting.'
      }
    })

    render(<ManagerEvents initialEvents={mockEvents} />)
    
    const decisionBtn = screen.getByText('Allocate Scouting')
    fireEvent.click(decisionBtn)
    
    expect(makeDecision).toHaveBeenCalledWith(1, 'ALLOCATE_SCOUTING')
    
    await waitFor(() => {
      expect(screen.queryByText('Allocate Scouting')).not.toBeInTheDocument()
      expect(screen.getByText('Successfully allocated scouting.')).toBeInTheDocument()
    })
  })

  test('handles API failure during submission', async () => {
    makeDecision.mockRejectedValue({ response: { data: { message: 'Server error' } } })

    render(<ManagerEvents initialEvents={mockEvents} />)
    
    const decisionBtn = screen.getByText('Allocate Scouting')
    fireEvent.click(decisionBtn)
    
    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument()
    })
  })
})
